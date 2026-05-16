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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MavenRepositoryTest {

  private Path testRoot;
  private Path localRepoDir;
  private Path remoteRepoDir;
  private Path settingsDir;
  private MavenRepository repository;

  @BeforeClass
  public void setUpRepository ()
    throws Exception {

    testRoot = Files.createTempDirectory("maven-repository-test");
    localRepoDir = Files.createDirectory(testRoot.resolve("local"));
    remoteRepoDir = Files.createDirectory(testRoot.resolve("remote"));
    settingsDir = Files.createDirectory(testRoot.resolve("settings"));

    publishArtifact("org.test", "alpha", "1.0.0", null);
    publishArtifact("org.test", "beta", "1.0.0", null);
    publishArtifact("org.test", "needs-alpha", "1.0.0",
      "<dependency><groupId>org.test</groupId><artifactId>alpha</artifactId><version>1.0.0</version></dependency>");
    publishArtifact("org.test", "alpha-optional", "1.0.0",
      "<dependency><groupId>org.test</groupId><artifactId>alpha</artifactId><version>1.0.0</version><optional>true</optional></dependency>");

    writeSettings();

    repository = new MavenRepository(settingsDir.toString(), "test", false);
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

  public void testAcquireArtifactResolvesAgainstFileRepository ()
    throws Exception {

    DefaultRepositorySystemSession session = repository.generateSession();
    Artifact resolved = repository.acquireArtifact(session, new MavenCoordinate("org.test", "alpha", "1.0.0"));

    Assert.assertEquals(resolved.getGroupId(), "org.test");
    Assert.assertEquals(resolved.getArtifactId(), "alpha");
    Assert.assertEquals(resolved.getVersion(), "1.0.0");
    Assert.assertNotNull(resolved.getFile());
    Assert.assertTrue(resolved.getFile().exists());
    Assert.assertTrue(resolved.getFile().toPath().startsWith(localRepoDir));
  }

  public void testAcquireArtifactThrowsWhenCoordinateMissing () {

    DefaultRepositorySystemSession session = repository.generateSession();

    Assert.assertThrows(ArtifactResolutionException.class, () ->
                                                             repository.acquireArtifact(session, new MavenCoordinate("org.test", "does-not-exist", "1.0.0")));
  }

  public void testResolveReturnsTransitiveClosure ()
    throws Exception {

    DefaultRepositorySystemSession session = repository.generateSession();
    Artifact root = repository.acquireArtifact(session, new MavenCoordinate("org.test", "needs-alpha", "1.0.0"));
    Artifact[] closure = repository.resolve(session, root);

    Set<String> artifactIds = new HashSet<>();
    for (Artifact artifact : closure) {
      artifactIds.add(artifact.getArtifactId());
      Assert.assertNotNull(artifact.getFile(), "Each artifact in the closure should have a file");
    }

    Assert.assertTrue(artifactIds.contains("needs-alpha"));
    Assert.assertTrue(artifactIds.contains("alpha"));
  }

  public void testResolveSkipsOptionalDependencies ()
    throws Exception {

    DefaultRepositorySystemSession session = repository.generateSession();
    Artifact root = repository.acquireArtifact(session, new MavenCoordinate("org.test", "alpha-optional", "1.0.0"));
    Artifact[] closure = repository.resolve(session, root);

    Set<String> artifactIds = new HashSet<>();
    for (Artifact artifact : closure) {
      artifactIds.add(artifact.getArtifactId());
    }

    Assert.assertTrue(artifactIds.contains("alpha-optional"));
    Assert.assertFalse(artifactIds.contains("alpha"),
      "Optional dependency should be filtered out of the resolved closure");
  }

  public void testGenerateSessionUsesConfiguredLocalRepository ()
    throws IOException {

    DefaultRepositorySystemSession session = repository.generateSession();

    Assert.assertNotNull(session.getLocalRepositoryManager());
    Assert.assertEquals(
      session.getLocalRepositoryManager().getRepository().getBasedir().getCanonicalFile(),
      localRepoDir.toFile().getCanonicalFile());
  }

  private void publishArtifact (String groupId, String artifactId, String version, String dependenciesXml)
    throws IOException {

    Path artifactDir = remoteRepoDir
                         .resolve(groupId.replace('.', '/'))
                         .resolve(artifactId)
                         .resolve(version);

    Files.createDirectories(artifactDir);

    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                   + "  <modelVersion>4.0.0</modelVersion>\n"
                   + "  <groupId>" + groupId + "</groupId>\n"
                   + "  <artifactId>" + artifactId + "</artifactId>\n"
                   + "  <version>" + version + "</version>\n"
                   + "  <packaging>jar</packaging>\n"
                   + ((dependenciesXml == null) ? "" : "  <dependencies>" + dependenciesXml + "</dependencies>\n")
                   + "</project>\n";

    Files.writeString(artifactDir.resolve(artifactId + "-" + version + ".pom"), pom);
    Files.write(artifactDir.resolve(artifactId + "-" + version + ".jar"),
      new byte[] {0x50, 0x4B, 0x03, 0x04});
  }

  private void writeSettings ()
    throws IOException {

    String remoteUrl = remoteRepoDir.toUri().toString();
    String settings = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\">\n"
                        + "  <localRepository>" + localRepoDir.toAbsolutePath() + "</localRepository>\n"
                        + "  <profiles>\n"
                        + "    <profile>\n"
                        + "      <id>test</id>\n"
                        + "      <activation><activeByDefault>true</activeByDefault></activation>\n"
                        + "      <repositories>\n"
                        + "        <repository>\n"
                        + "          <id>test-remote</id>\n"
                        + "          <url>" + remoteUrl + "</url>\n"
                        + "          <layout>default</layout>\n"
                        + "          <releases><enabled>true</enabled></releases>\n"
                        + "          <snapshots><enabled>false</enabled></snapshots>\n"
                        + "        </repository>\n"
                        + "      </repositories>\n"
                        + "    </profile>\n"
                        + "  </profiles>\n"
                        + "</settings>\n";

    Files.writeString(settingsDir.resolve("settings.xml"), settings);
  }
}
