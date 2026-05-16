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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MavenScannerTest {

  private Path testRoot;
  private Path settingsDir;

  @BeforeClass
  public void setUp ()
    throws IOException {

    testRoot = Files.createTempDirectory("maven-scanner-test");
    settingsDir = Files.createDirectory(testRoot.resolve("settings"));
    Path localRepo = Files.createDirectory(testRoot.resolve("local"));

    String settings = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\">\n"
                        + "  <localRepository>" + localRepo.toAbsolutePath() + "</localRepository>\n"
                        + "</settings>\n";

    Files.writeString(settingsDir.resolve("settings.xml"), settings);
  }

  @AfterClass(alwaysRun = true)
  public void tearDown ()
    throws IOException {

    if ((testRoot != null) && Files.exists(testRoot)) {
      Files.walk(testRoot)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }
  }

  public void testStartPerformsInitialScanAndNotifiesListeners ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenCoordinate coord = new MavenCoordinate("org.test", "lib", "1.0.0");
    Artifact artifact = artifact("org.test:lib:1.0.0", 1_700_000_000_000L);
    repo.publish(coord, artifact);

    AtomicReference<MavenScannerEvent> captured = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    MavenScanner scanner = new MavenScanner(repo, new Stint(10, TimeUnit.SECONDS), coord);
    scanner.addMavenScannerListener(event -> {
      captured.set(event);
      latch.countDown();
    });

    try {
      scanner.start();
      Assert.assertTrue(latch.await(2, TimeUnit.SECONDS), "Initial scan should notify listeners");

      MavenScannerEvent event = captured.get();
      Assert.assertSame(event.getSource(), scanner);
      Assert.assertEquals(event.getArtifacts().length, 1);
      Assert.assertSame(event.getArtifacts()[0], artifact);
      Assert.assertEquals(event.getArtifactDeltaMap().size(), 1);
      Assert.assertTrue(event.getArtifactDeltaMap().containsKey(artifact));
      Assert.assertNull(event.getArtifactDeltaMap().get(artifact),
        "Prior artifact for first observation should be null");
      Assert.assertNotNull(event.getClassLoader());
    } finally {
      scanner.stop();
    }
  }

  public void testStartPropagatesResolutionFailure ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenScanner scanner = new MavenScanner(repo, new Stint(10, TimeUnit.SECONDS),
      new MavenCoordinate("org.test", "missing", "1.0.0"));

    Assert.assertThrows(ArtifactResolutionException.class, scanner::start);
  }

  public void testReleaseFileChangeWithSameIdentityDoesNotRenotify ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenCoordinate coord = new MavenCoordinate("org.test", "lib", "1.0.0");
    File jar = tempJar("release-stable", 1_700_000_000_000L);
    repo.publish(coord, new DefaultArtifact("org.test:lib:1.0.0").setFile(jar));

    AtomicInteger notifications = new AtomicInteger();
    CountDownLatch firstScan = new CountDownLatch(1);
    MavenScanner scanner = new MavenScanner(repo, new Stint(50, TimeUnit.MILLISECONDS), coord);

    scanner.addMavenScannerListener(event -> {
      notifications.incrementAndGet();
      firstScan.countDown();
    });

    try {
      scanner.start();
      Assert.assertTrue(firstScan.await(2, TimeUnit.SECONDS));

      Assert.assertTrue(jar.setLastModified(1_800_000_000_000L));
      Thread.sleep(500);

      Assert.assertEquals(notifications.get(), 1,
        "Release identity-only artifact should not renotify on file mtime change");
    } finally {
      scanner.stop();
    }
  }

  public void testSnapshotFileMtimeChangeTriggersRenotification ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenCoordinate coord = new MavenCoordinate("org.test", "lib", "1.0.0-SNAPSHOT");
    File jar = tempJar("snap-stable", 1_700_000_000_000L);
    repo.publish(coord, new DefaultArtifact("org.test:lib:1.0.0-SNAPSHOT").setFile(jar));

    CountDownLatch initialScan = new CountDownLatch(1);
    CountDownLatch secondScan = new CountDownLatch(2);
    AtomicInteger notifications = new AtomicInteger();
    MavenScanner scanner = new MavenScanner(repo, new Stint(50, TimeUnit.MILLISECONDS), coord);

    scanner.addMavenScannerListener(event -> {
      notifications.incrementAndGet();
      initialScan.countDown();
      secondScan.countDown();
    });

    try {
      scanner.start();
      Assert.assertTrue(initialScan.await(2, TimeUnit.SECONDS));

      Assert.assertTrue(jar.setLastModified(1_800_000_000_000L));

      Assert.assertTrue(secondScan.await(2, TimeUnit.SECONDS),
        "Snapshot with new file mtime should trigger renotification");
      Assert.assertTrue(notifications.get() >= 2);
    } finally {
      scanner.stop();
    }
  }

  public void testStartIsIdempotent ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenCoordinate coord = new MavenCoordinate("org.test", "lib", "1.0.0");
    repo.publish(coord, artifact("org.test:lib:1.0.0", 1_700_000_000_000L));

    AtomicInteger notifications = new AtomicInteger();
    CountDownLatch latch = new CountDownLatch(1);
    MavenScanner scanner = new MavenScanner(repo, new Stint(10, TimeUnit.SECONDS), coord);

    scanner.addMavenScannerListener(event -> {
      notifications.incrementAndGet();
      latch.countDown();
    });

    try {
      scanner.start();
      Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
      scanner.start();

      Thread.sleep(150);
      Assert.assertEquals(notifications.get(), 1, "Second start() should be a no-op");
    } finally {
      scanner.stop();
    }
  }

  public void testStopBeforeStartIsNoOp ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenScanner scanner = new MavenScanner(repo, new Stint(10, TimeUnit.SECONDS),
      new MavenCoordinate("org.test", "lib", "1.0.0"));

    scanner.stop();
  }

  public void testRemovedListenerIsNotNotified ()
    throws Exception {

    FakeMavenRepository repo = newRepository();
    MavenCoordinate coord = new MavenCoordinate("org.test", "lib", "1.0.0");
    repo.publish(coord, artifact("org.test:lib:1.0.0", 1_700_000_000_000L));

    AtomicInteger kept = new AtomicInteger();
    AtomicInteger removed = new AtomicInteger();
    CountDownLatch latch = new CountDownLatch(1);
    MavenScannerListener keepListener = event -> {
      kept.incrementAndGet();
      latch.countDown();
    };
    MavenScannerListener removedListener = event -> removed.incrementAndGet();

    MavenScanner scanner = new MavenScanner(repo, new Stint(10, TimeUnit.SECONDS), coord);
    scanner.addMavenScannerListener(keepListener);
    scanner.addMavenScannerListener(removedListener);
    scanner.removeMavenScannerListener(removedListener);

    try {
      scanner.start();
      Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));

      Assert.assertEquals(kept.get(), 1);
      Assert.assertEquals(removed.get(), 0);
    } finally {
      scanner.stop();
    }
  }

  public void testConstructorRejectsNullCycleStint ()
    throws Exception {

    FakeMavenRepository repo = newRepository();

    Assert.assertThrows(IllegalArgumentException.class, () ->
                                                          new MavenScanner(repo, null, new MavenCoordinate("g", "a", "1")));
  }

  public void testConstructorRejectsNullCoordinatesArray ()
    throws Exception {

    FakeMavenRepository repo = newRepository();

    Assert.assertThrows(IllegalArgumentException.class, () ->
                                                          new MavenScanner(repo, new Stint(1, TimeUnit.SECONDS), (MavenCoordinate[])null));
  }

  private FakeMavenRepository newRepository ()
    throws SettingsBuildingException {

    return new FakeMavenRepository(settingsDir);
  }

  private Artifact artifact (String coords, long mtime)
    throws IOException {

    return new DefaultArtifact(coords).setFile(tempJar("scanner-test", mtime));
  }

  private File tempJar (String prefix, long mtime)
    throws IOException {

    File jar = Files.createTempFile(testRoot, prefix, ".jar").toFile();
    Assert.assertTrue(jar.setLastModified(mtime));

    return jar;
  }

  private static class FakeMavenRepository extends MavenRepository {

    private final ConcurrentMap<MavenCoordinate, Artifact> published = new ConcurrentHashMap<>();

    FakeMavenRepository (Path settingsDir)
      throws SettingsBuildingException {

      super(settingsDir.toString(), "test", true);
    }

    void publish (MavenCoordinate coordinate, Artifact artifact) {

      published.put(coordinate, artifact);
    }

    @Override
    public Artifact acquireArtifact (DefaultRepositorySystemSession session, MavenCoordinate mavenCoordinate)
      throws ArtifactResolutionException {

      Artifact artifact = published.get(mavenCoordinate);

      if (artifact == null) {
        throw new ArtifactResolutionException(Collections.emptyList(),
          "no fake artifact published for coordinate " + mavenCoordinate.getGroupId() + ":"
            + mavenCoordinate.getArtifactId() + ":" + mavenCoordinate.getVersion());
      }

      return artifact;
    }

    @Override
    public Artifact[] resolve (DefaultRepositorySystemSession session, Artifact artifact) {

      return new Artifact[] {artifact};
    }
  }
}
