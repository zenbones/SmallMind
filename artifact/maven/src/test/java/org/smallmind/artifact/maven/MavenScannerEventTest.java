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
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MavenScannerEventTest {

  public void testAccessorsReturnConstructorArguments () {

    Object source = new Object();
    Artifact current = new DefaultArtifact("org.example:lib:1.0.0");
    Map<Artifact, Artifact> deltaMap = new HashMap<>();
    deltaMap.put(current, null);
    ArtifactTag[] tags = new ArtifactTag[] {new ArtifactTag(current)};
    ClassLoader loader = getClass().getClassLoader();

    MavenScannerEvent event = new MavenScannerEvent(source, deltaMap, tags, loader);

    Assert.assertSame(event.getSource(), source);
    Assert.assertSame(event.getArtifactDeltaMap(), deltaMap);
    Assert.assertSame(event.getClassLoader(), loader);
  }

  public void testArtifactsDerivedFromTagArray () {

    Artifact first = new DefaultArtifact("org.example:first:1.0.0");
    Artifact second = new DefaultArtifact("org.example:second:2.0.0");
    ArtifactTag[] tags = new ArtifactTag[] {new ArtifactTag(first), new ArtifactTag(second)};

    MavenScannerEvent event = new MavenScannerEvent(this, new HashMap<>(), tags, getClass().getClassLoader());

    Artifact[] artifacts = event.getArtifacts();

    Assert.assertEquals(artifacts.length, 2);
    Assert.assertSame(artifacts[0], first);
    Assert.assertSame(artifacts[1], second);
  }

  public void testArtifactsArrayPreservesNullSlots () {

    Artifact resolved = new DefaultArtifact("org.example:lib:1.0.0");
    ArtifactTag[] tags = new ArtifactTag[] {null, new ArtifactTag(resolved), null};

    MavenScannerEvent event = new MavenScannerEvent(this, new HashMap<>(), tags, getClass().getClassLoader());

    Artifact[] artifacts = event.getArtifacts();

    Assert.assertEquals(artifacts.length, 3);
    Assert.assertNull(artifacts[0]);
    Assert.assertSame(artifacts[1], resolved);
    Assert.assertNull(artifacts[2]);
  }

  public void testEmptyTagArrayProducesEmptyArtifactsArray () {

    MavenScannerEvent event = new MavenScannerEvent(this, new HashMap<>(), new ArtifactTag[0], getClass().getClassLoader());

    Assert.assertEquals(event.getArtifacts().length, 0);
  }
}
