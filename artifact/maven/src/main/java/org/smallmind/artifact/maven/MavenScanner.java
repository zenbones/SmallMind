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
 * Periodically polls configured Maven coordinates for updates and notifies listeners when new artifacts or versions are detected.
 * Each scan resolves dependencies, builds a class loader over updated artifacts, and emits a {@link MavenScannerEvent}.
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
   * Creates a scanner using the default settings directory.
   *
   * @param repositoryId     identifier for outbound repository requests.
   * @param offline          whether remote repositories should be contacted.
   * @param cycleStint       interval between scans.
   * @param mavenCoordinates coordinates to monitor for updates.
   * @throws SettingsBuildingException if settings cannot be loaded.
   */
  public MavenScanner (String repositoryId, boolean offline, Stint cycleStint, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(repositoryId, offline), cycleStint, mavenCoordinates);
  }

  /**
   * Creates a scanner with an explicit settings directory.
   *
   * @param settingsDirectory directory containing {@code settings.xml}; defaults to {@code ~/.m2} when null or empty.
   * @param repositoryId      identifier for outbound repository requests.
   * @param offline           whether remote repositories should be contacted.
   * @param cycleStint        interval between scans.
   * @param mavenCoordinates  coordinates to monitor for updates.
   * @throws SettingsBuildingException if settings cannot be loaded.
   */
  public MavenScanner (String settingsDirectory, String repositoryId, boolean offline, Stint cycleStint, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(settingsDirectory, repositoryId, offline), cycleStint, mavenCoordinates);
  }

  /**
   * Internal constructor used by public variants.
   *
   * @param mavenRepository  backing repository used for resolution.
   * @param cycleStint       interval between scans.
   * @param mavenCoordinates coordinates to monitor for updates.
   * @throws IllegalArgumentException if stint or coordinates are missing.
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
   * Registers a listener that will be invoked when artifacts change.
   *
   * @param listener listener to register.
   */
  public synchronized void addMavenScannerListener (MavenScannerListener listener) {

    listenerList.add(listener);
  }

  /**
   * Unregisters a previously added listener.
   *
   * @param listener listener to remove.
   */
  public synchronized void removeMavenScannerListener (MavenScannerListener listener) {

    listenerList.remove(listener);
  }

  /**
   * Starts periodic scanning if the component is stopped. Immediately performs an initial scan before scheduling repeats.
   *
   * @throws DependencyCollectionException if dependency metadata cannot be collected.
   * @throws DependencyResolutionException if any dependency fails to resolve during the initial scan.
   * @throws ArtifactResolutionException   if any monitored artifact cannot be resolved during the initial scan.
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
   * Stops periodic scanning and waits for the worker to exit if currently running.
   *
   * @throws InterruptedException if interrupted while waiting for the worker to stop.
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
   * Performs a single scan cycle: resolves each coordinate, detects changes, builds a new class loader, and notifies listeners.
   *
   * @throws DependencyCollectionException if dependency metadata cannot be collected.
   * @throws DependencyResolutionException if any dependency fails to resolve.
   * @throws ArtifactResolutionException   if a monitored artifact fails to resolve.
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
   * Worker that performs repeated scans at the configured interval until signaled to stop.
   */
  private class ScannerWorker implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    /**
     * Signals the worker to finish and waits for confirmation of exit.
     *
     * @throws InterruptedException if interrupted while awaiting termination.
     */
    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    /**
     * Executes scan cycles until stopped or interrupted, logging and continuing past individual scan errors.
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
