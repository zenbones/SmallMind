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
package org.smallmind.spark.singularity.maven;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

@org.testng.annotations.Test(groups = "unit")
public class AscArtifactMetadataTest {

  private Path repositoryBase;

  @BeforeMethod
  public void createRepositoryBase ()
    throws Exception {

    repositoryBase = Files.createTempDirectory("asc-local-repository");
  }

  @AfterMethod(alwaysRun = true)
  public void deleteRepositoryBase ()
    throws Exception {

    if (repositoryBase != null) {
      try (java.util.stream.Stream<Path> walk = Files.walk(repositoryBase)) {
        walk.sorted(java.util.Comparator.reverseOrder()).forEach((path) -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception exception) {
            // best effort
          }
        });
      }
    }
  }

  private MavenArtifactRepository localRepository () {

    MavenArtifactRepository repository = new MavenArtifactRepository();

    repository.setLayout(new DefaultRepositoryLayout());
    repository.setUrl(repositoryBase.toUri().toString());

    return repository;
  }

  private static Artifact artifact (String classifier) {

    return new DefaultArtifact("org.smallmind", "widget", "1.2.0", "compile", "jar", classifier, new DefaultArtifactHandler("jar"));
  }

  public void testFilenameIsDerivedFromCoordinatesWithoutClassifier () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), Path.of("widget.jar.asc"));

    Assert.assertEquals(metadata.getRemoteFilename(), "widget-1.2.0.jar.asc");
    Assert.assertEquals(metadata.getLocalFilename(null), "widget-1.2.0.jar.asc");
  }

  public void testFilenameIncludesTheClassifierWhenPresent () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact("tests"), Path.of("widget.jar.asc"));

    Assert.assertEquals(metadata.getRemoteFilename(), "widget-1.2.0-tests.jar.asc");
  }

  public void testKeyDistinguishesTypeAndClassifier () {

    Assert.assertEquals(new AscArtifactMetadata(artifact(null), Path.of("x")).getKey(), "gpg signature org.smallmind:widget:jar:null");
    Assert.assertEquals(new AscArtifactMetadata(artifact("tests"), Path.of("x")).getKey(), "gpg signature org.smallmind:widget:jar:tests");
  }

  public void testBaseVersionAndVersionDirectoryFlagComeFromTheArtifact () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), Path.of("x"));

    Assert.assertEquals(metadata.getBaseVersion(), "1.2.0");
    Assert.assertTrue(metadata.storedInArtifactVersionDirectory());
  }

  public void testPathIsRetained () {

    Path path = Path.of("some", "where", "widget.jar.asc");

    Assert.assertEquals(new AscArtifactMetadata(artifact(null), path).getPath(), path);
  }

  // Merging two records that point at the same signature file is a no-op.
  public void testMergeWithMatchingPathIsHarmless () {

    Path path = Path.of("widget.jar.asc");

    new AscArtifactMetadata(artifact(null), path).merge(new AscArtifactMetadata(artifact(null), path));
  }

  // Two distinct signature files under the same key is a configuration error.
  public void testMergeWithDifferingPathIsRejected () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), Path.of("widget.jar.asc"));

    Assert.assertThrows(IllegalStateException.class, () -> metadata.merge(new AscArtifactMetadata(artifact(null), Path.of("other.jar.asc"))));
  }

  // The legacy-API merge overload enforces the same single-signature-per-key rule.
  public void testLegacyMergeWithMatchingPathIsHarmless () {

    Path path = Path.of("widget.jar.asc");

    new AscArtifactMetadata(artifact(null), path).merge((org.apache.maven.repository.legacy.metadata.ArtifactMetadata)new AscArtifactMetadata(artifact(null), path));
  }

  public void testLegacyMergeWithDifferingPathIsRejected () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), Path.of("widget.jar.asc"));

    Assert.assertThrows(IllegalStateException.class, () -> metadata.merge((org.apache.maven.repository.legacy.metadata.ArtifactMetadata)new AscArtifactMetadata(artifact(null), Path.of("other.jar.asc"))));
  }

  // The signature file is copied to the layout-computed location under the local repository.
  public void testStoreInLocalRepositoryCopiesTheSignatureFile ()
    throws Exception {

    Path signature = Files.createTempFile("widget", ".jar.asc");

    Files.writeString(signature, "PGP SIGNATURE", StandardCharsets.UTF_8);

    try {

      AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), signature);
      MavenArtifactRepository repository = localRepository();
      Path destination = Paths.get(repository.getBasedir(), repository.pathOfLocalRepositoryMetadata(metadata, null));

      Files.createDirectories(destination.getParent());
      metadata.storeInLocalRepository(repository, null);

      Assert.assertTrue(Files.isRegularFile(destination));
      Assert.assertEquals(Files.readString(destination, StandardCharsets.UTF_8), "PGP SIGNATURE");
    } finally {
      Files.deleteIfExists(signature);
    }
  }

  // A failure to copy (here, a missing source file) is surfaced as a RepositoryMetadataStoreException.
  public void testStoreInLocalRepositoryWrapsCopyFailures () {

    AscArtifactMetadata metadata = new AscArtifactMetadata(artifact(null), Path.of("does-not-exist.jar.asc"));

    Assert.assertThrows(RepositoryMetadataStoreException.class, () -> metadata.storeInLocalRepository(localRepository(), null));
  }
}
