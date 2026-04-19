/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.artifact.maven;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.smallmind.nutsnbolts.lang.ClassGate;
import org.smallmind.nutsnbolts.lang.ClasspathClassGate;
import org.smallmind.nutsnbolts.lang.GatingClassLoader;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Polling component that monitors a set of Maven coordinates for artifact changes and notifies
 * registered listeners when an update is detected.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Construct a scanner and register at least one {@link MavenScannerListener} via
 *       {@link #addMavenScannerListener}.</li>
 *   <li>Call {@link #start()}, which performs an immediate first scan (throwing on resolution
 *       failure) and then launches a daemon worker thread that repeats scans at the configured
 *       {@link Stint} interval.</li>
 *   <li>Call {@link #stop()} to signal the worker and block until it exits cleanly.</li>
 * </ol>
 *
 * <h3>Change detection</h3>
 * <p>Each scan cycle resolves every monitored coordinate against the configured Maven repositories.
 * The resolved artifact is wrapped in an {@link ArtifactTag} that records the file's current
 * last-modified time and compared to the tag stored from the previous cycle.  Release artifacts
 * are compared by identity only; snapshot artifacts are also compared by file timestamp so that
 * a re-deployed snapshot triggers a notification even when the version string is unchanged.
 *
 * <h3>Notification</h3>
 * <p>When at least one coordinate has changed, the scanner resolves all transitive compile-scope
 * dependencies of the changed artifacts, assembles a {@link GatingClassLoader} over the resulting
 * file set, and delivers a {@link MavenScannerEvent} to every registered listener.  Scans that
 * detect no changes produce no notification.
 *
 * <h3>Error handling</h3>
 * <p>Resolution errors during periodic scans (after the initial one) are logged and swallowed so
 * that a transient network failure does not terminate the scanner.  The stored tags are left
 * unchanged, meaning a failed scan is treated as if nothing changed.
 *
 * <p>The public lifecycle methods ({@link #start()}, {@link #stop()},
 * {@link #addMavenScannerListener}, {@link #removeMavenScannerListener}) are {@code synchronized}.
 * The internal {@link #updateArtifact()} method is also {@code synchronized} to prevent a
 * concurrent stop from racing with an ongoing scan.
 */
public class MavenScanner {

  private final LinkedList<MavenScannerListener> listenerList = new LinkedList<>();
  private final Stint cycleStint;
  private final MavenRepository mavenRepository;
  private final MavenCoordinate[] mavenCoordinates;
  private final ArtifactTag[] artifactTags;
  private ScannerWorker scannerWorker;
  private ComponentStatus status = ComponentStatus.STOPPED;

  /**
   * Creates a scanner backed by the Maven settings found in {@code ~/.m2/settings.xml}.
   *
   * @param repositoryId     short identifier embedded in outbound {@code User-Agent} headers
   * @param offline          {@code true} to restrict resolution to locally cached artifacts
   * @param cycleStint       time to wait between successive scan cycles; must not be {@code null}
   * @param mavenCoordinates one or more coordinates to monitor; must not be {@code null}
   * @throws SettingsBuildingException if {@code ~/.m2/settings.xml} cannot be loaded
   * @throws IllegalArgumentException  if {@code cycleStint} or {@code mavenCoordinates} is
   *                                   {@code null}
   */
  public MavenScanner (String repositoryId, boolean offline, Stint cycleStint, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(repositoryId, offline), cycleStint, mavenCoordinates);
  }

  /**
   * Creates a scanner backed by the Maven settings found in the given directory.
   *
   * @param settingsDirectory directory containing {@code settings.xml}; {@code null} or empty
   *                          falls back to {@code ~/.m2}
   * @param repositoryId      short identifier embedded in outbound {@code User-Agent} headers
   * @param offline           {@code true} to restrict resolution to locally cached artifacts
   * @param cycleStint        time to wait between successive scan cycles; must not be {@code null}
   * @param mavenCoordinates  one or more coordinates to monitor; must not be {@code null}
   * @throws SettingsBuildingException if the settings file cannot be loaded
   * @throws IllegalArgumentException  if {@code cycleStint} or {@code mavenCoordinates} is
   *                                   {@code null}
   */
  public MavenScanner (String settingsDirectory, String repositoryId, boolean offline, Stint cycleStint, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(settingsDirectory, repositoryId, offline), cycleStint, mavenCoordinates);
  }

  /**
   * Shared internal constructor that validates arguments and initialises the tag array.
   *
   * @param mavenRepository  backing repository to use for all resolution operations
   * @param cycleStint       polling interval; must not be {@code null}
   * @param mavenCoordinates coordinates to monitor; must not be {@code null}
   * @throws IllegalArgumentException if {@code cycleStint} or {@code mavenCoordinates} is
   *                                  {@code null}
   */
  private MavenScanner (MavenRepository mavenRepository, Stint cycleStint, MavenCoordinate... mavenCoordinates) {

    if (cycleStint == null) {
      throw new IllegalArgumentException("Must provide a cycle duration");
    }
    if (mavenCoordinates == null) {
      throw new IllegalArgumentException("Must provide some maven coordinates");
    }

    this.mavenRepository = mavenRepository;
    this.cycleStint = cycleStint;
    this.mavenCoordinates = mavenCoordinates;

    artifactTags = new ArtifactTag[mavenCoordinates.length];
  }

  /**
   * Registers a listener to receive artifact-change notifications.
   *
   * <p>Listeners are invoked in registration order on the internal worker thread.  Adding the
   * same listener instance more than once will result in duplicate notifications.
   *
   * @param listener listener to register; must not be {@code null}
   */
  public synchronized void addMavenScannerListener (MavenScannerListener listener) {

    listenerList.add(listener);
  }

  /**
   * Removes the first occurrence of the given listener from the notification list.
   *
   * <p>If the listener was registered multiple times, only one registration is removed per call.
   * Has no effect if the listener is not currently registered.
   *
   * @param listener listener to remove
   */
  public synchronized void removeMavenScannerListener (MavenScannerListener listener) {

    listenerList.remove(listener);
  }

  /**
   * Starts the scanner if it is currently stopped.
   *
   * <p>An initial scan is performed synchronously before the background worker is launched.
   * If the initial scan detects any changes (which it will on first start, since all tags are
   * initially {@code null}) listeners are notified immediately on the calling thread.  Subsequent
   * scans run on a daemon thread at the configured {@link Stint} interval.
   *
   * <p>If the scanner is already started this method is a no-op.
   *
   * @throws DependencyCollectionException if dependency metadata cannot be collected during
   *                                       the initial scan
   * @throws DependencyResolutionException if a dependency fails to resolve during the initial scan
   * @throws ArtifactResolutionException   if a monitored artifact cannot be resolved during the
   *                                       initial scan
   */
  public synchronized void start ()
    throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {

    if (status.equals(ComponentStatus.STOPPED)) {

      Thread workerThread;

      updateArtifact();

      workerThread = new Thread(scannerWorker = new ScannerWorker());
      workerThread.setDaemon(true);
      workerThread.start();

      status = ComponentStatus.STARTED;
    }
  }

  /**
   * Stops the scanner and blocks until the background worker thread has exited.
   *
   * <p>If the scanner is already stopped this method is a no-op.  In-progress scan cycles are
   * allowed to complete before the worker exits.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the
   *                              worker to exit
   */
  public synchronized void stop ()
    throws InterruptedException {

    if (status.equals(ComponentStatus.STARTED)) {
      if (scannerWorker != null) {
        scannerWorker.stop();
      }

      status = ComponentStatus.STOPPED;
    }
  }

  /**
   * Executes one scan cycle: resolves each monitored coordinate, compares the result against the
   * stored tag, and if any coordinate has changed, resolves dependencies, builds a class loader,
   * and notifies all registered listeners.
   *
   * <p>A new repository session is created at the start of each call.  Coordinates whose
   * {@link ArtifactTag} is unchanged are skipped during dependency resolution.  All changed
   * artifacts share a single {@link GatingClassLoader} whose classpath is the union of their
   * transitive dependency sets (deduplication applied).
   *
   * <p>This method is {@code synchronized} to serialise concurrent calls from {@link #start()}
   * and the worker thread.
   *
   * @throws DependencyCollectionException if dependency metadata cannot be collected for a
   *                                       changed artifact
   * @throws DependencyResolutionException if a transitive dependency of a changed artifact cannot
   *                                       be resolved
   * @throws ArtifactResolutionException   if a monitored coordinate itself cannot be resolved
   */
  private synchronized void updateArtifact ()
    throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {

    DefaultRepositorySystemSession session = mavenRepository.generateSession();
    MavenScannerEvent event;
    HashMap<Artifact, Artifact> artifactDeltaMap = new HashMap<>();
    HashSet<Artifact> dependentArtifactSet = new HashSet<>();
    LinkedList<ClassGate> classGateList = new LinkedList<>();

    for (int index = 0; index < mavenCoordinates.length; index++) {

      ArtifactTag currentArtifactTag = new ArtifactTag(mavenRepository.acquireArtifact(session, mavenCoordinates[index]));

      if (!currentArtifactTag.equals(artifactTags[index])) {

        Artifact[] dependentArtifacts = mavenRepository.resolve(session, currentArtifactTag.getArtifact());

        for (Artifact dependentArtifact : dependentArtifacts) {
          if (dependentArtifactSet.add(dependentArtifact)) {
            classGateList.add(new ClasspathClassGate(dependentArtifact.getFile().getAbsolutePath()));
          }
        }

        artifactDeltaMap.put(currentArtifactTag.getArtifact(), (artifactTags[index] == null) ? null : artifactTags[index].getArtifact());
        artifactTags[index] = currentArtifactTag;
      }
    }

    if (!classGateList.isEmpty()) {

      GatingClassLoader gatingClassLoader;
      ClassGate[] classGates;

      classGates = new ClassGate[classGateList.size()];
      classGateList.toArray(classGates);

      gatingClassLoader = new GatingClassLoader(Thread.currentThread().getContextClassLoader(), -1, classGates);
      event = new MavenScannerEvent(this, artifactDeltaMap, artifactTags, gatingClassLoader);

      for (MavenScannerListener listener : listenerList) {
        listener.artifactChange(event);
      }
    }
  }

  /**
   * Background worker that drives periodic scan cycles at the configured interval.
   *
   * <p>Uses two {@link CountDownLatch latches}:
   * <ul>
   *   <li>{@code finishLatch} — counted down by {@link #stop()} to signal the worker to exit
   *       after the current cycle completes.</li>
   *   <li>{@code exitLatch} — counted down by the worker immediately before its thread exits,
   *       allowing {@link #stop()} to block until termination is confirmed.</li>
   * </ul>
   *
   * <p>Exceptions thrown by individual scan cycles are logged at ERROR level and swallowed;
   * the worker continues until explicitly stopped or the thread is interrupted.
   */
  private class ScannerWorker implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    /**
     * Signals this worker to stop after the current scan cycle and blocks until the worker
     * thread confirms exit.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting for the
     *                              worker to exit
     */
    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    /**
     * Main scan loop: invokes {@link MavenScanner#updateArtifact()} repeatedly, waiting
     * {@link MavenScanner#cycleStint} between iterations.
     *
     * <p>Exceptions from a scan cycle are logged and the loop continues.  An
     * {@link InterruptedException} from the inter-cycle wait exits the loop and ensures
     * {@code finishLatch} is counted down so that a concurrent {@link #stop()} call does not
     * block indefinitely.  The {@code exitLatch} is always counted down in the {@code finally}
     * block regardless of how the loop terminates.
     */
    @Override
    public void run () {

      try {
        do {
          try {
            updateArtifact();
          } catch (Exception exception) {
            LoggerManager.getLogger(MavenScanner.class).error(exception);
          }
        } while (!finishLatch.await(cycleStint.getTime(), cycleStint.getTimeUnit()));
      } catch (InterruptedException interruptedException) {
        finishLatch.countDown();
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
