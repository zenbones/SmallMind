/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.scribe.pen.LoggerManager;

public class MavenScanner {

  private static enum State {STARTED, STOPPED}

  private final LinkedList<MavenScannerListener> listenerList = new LinkedList<>();
  private final Duration cycleDuration;
  private final MavenRepository mavenRepository;
  private final MavenCoordinate[] mavenCoordinates;
  private final ArtifactTag[] artifactTags;
  private ScannerWorker scannerWorker;
  private State state = State.STOPPED;

  public MavenScanner (String repositoryId, boolean offline, Duration cycleDuration, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(repositoryId, offline), cycleDuration, mavenCoordinates);
  }

  public MavenScanner (String settingsDirectory, String repositoryId, boolean offline, Duration cycleDuration, MavenCoordinate... mavenCoordinates)
    throws SettingsBuildingException {

    this(new MavenRepository(settingsDirectory, repositoryId, offline), cycleDuration, mavenCoordinates);
  }

  private MavenScanner (MavenRepository mavenRepository, Duration cycleDuration, MavenCoordinate... mavenCoordinates) {

    if (cycleDuration == null) {
      throw new IllegalArgumentException("Must provide a cycle duration");
    }
    if (mavenCoordinates == null) {
      throw new IllegalArgumentException("Must provide some maven coordinates");
    }

    this.mavenRepository = mavenRepository;
    this.cycleDuration = cycleDuration;
    this.mavenCoordinates = mavenCoordinates;

    artifactTags = new ArtifactTag[mavenCoordinates.length];
  }

  public synchronized void addMavenScannerListener (MavenScannerListener listener) {

    listenerList.add(listener);
  }

  public synchronized void removeMavenScannerListener (MavenScannerListener listener) {

    listenerList.remove(listener);
  }

  public synchronized void start ()
    throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException {

    if (state.equals(State.STOPPED)) {

      Thread workerThread;

      updateArtifact();

      workerThread = new Thread(scannerWorker = new ScannerWorker());
      workerThread.setDaemon(true);
      workerThread.start();

      state = State.STARTED;
    }
  }

  public synchronized void stop ()
    throws InterruptedException {

    if (state.equals(State.STARTED)) {
      if (scannerWorker != null) {
        scannerWorker.stop();
      }

      state = State.STOPPED;
    }
  }

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

  @Override
  protected void finalize ()
    throws InterruptedException {

    stop();
  }

  private class ScannerWorker implements Runnable {

    private CountDownLatch finishLatch = new CountDownLatch(1);
    private CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        do {
          try {
            updateArtifact();
          } catch (Exception exception) {
            LoggerManager.getLogger(MavenScanner.class).error(exception);
          }
        } while (!finishLatch.await(cycleDuration.getTime(), cycleDuration.getTimeUnit()));
      } catch (InterruptedException interruptedException) {
        finishLatch.countDown();
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
