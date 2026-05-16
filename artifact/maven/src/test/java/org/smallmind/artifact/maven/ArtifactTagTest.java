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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ArtifactTagTest {

  public void testConstructorRecordsZeroWhenArtifactHasNoFile () {

    Artifact release = new DefaultArtifact("org.example:lib:1.0.0");
    ArtifactTag tag = new ArtifactTag(release);

    Assert.assertSame(tag.getArtifact(), release);
    Assert.assertEquals(tag.getLastModTime(), 0L);
  }

  public void testConstructorRecordsFileLastModifiedTime ()
    throws IOException {

    File file = createTempArtifactFile("artifact-tag-mtime");
    Assert.assertTrue(file.setLastModified(1_700_000_000_000L));

    Artifact artifact = new DefaultArtifact("org.example:lib:1.0.0").setFile(file);
    ArtifactTag tag = new ArtifactTag(artifact);

    Assert.assertEquals(tag.getLastModTime(), file.lastModified());
  }

  public void testReleaseEqualityIgnoresLastModTime ()
    throws IOException {

    File file = createTempArtifactFile("release-stable");
    Assert.assertTrue(file.setLastModified(1_700_000_000_000L));

    Artifact release = new DefaultArtifact("org.example:lib:1.0.0").setFile(file);
    ArtifactTag earlierTag = new ArtifactTag(release);

    Assert.assertTrue(file.setLastModified(1_800_000_000_000L));
    ArtifactTag laterTag = new ArtifactTag(release);

    Assert.assertFalse(release.isSnapshot());
    Assert.assertNotEquals(earlierTag.getLastModTime(), laterTag.getLastModTime());
    Assert.assertEquals(earlierTag, laterTag);
  }

  public void testSnapshotEqualityRequiresMatchingLastModTime ()
    throws IOException {

    File file = createTempArtifactFile("snapshot-stable");
    Assert.assertTrue(file.setLastModified(1_700_000_000_000L));

    Artifact snapshot = new DefaultArtifact("org.example:lib:1.0.0-SNAPSHOT").setFile(file);
    ArtifactTag earlierTag = new ArtifactTag(snapshot);

    Assert.assertTrue(file.setLastModified(1_800_000_000_000L));
    ArtifactTag laterTag = new ArtifactTag(snapshot);

    Assert.assertTrue(file.setLastModified(1_700_000_000_000L));
    ArtifactTag earlierTwinTag = new ArtifactTag(snapshot);

    Assert.assertTrue(snapshot.isSnapshot());
    Assert.assertNotEquals(earlierTag, laterTag);
    Assert.assertEquals(earlierTag, earlierTwinTag);
  }

  public void testEqualsRejectsNonTag () {

    ArtifactTag tag = new ArtifactTag(new DefaultArtifact("org.example:lib:1.0.0"));

    Assert.assertNotEquals(tag, "not a tag");
  }

  private File createTempArtifactFile (String prefix)
    throws IOException {

    File file = Files.createTempFile(prefix, ".jar").toFile();
    file.deleteOnExit();

    return file;
  }
}
