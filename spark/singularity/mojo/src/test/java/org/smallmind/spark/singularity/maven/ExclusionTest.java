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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.testng.Assert;

@org.testng.annotations.Test(groups = "unit")
public class ExclusionTest {

  private static Artifact artifact (String groupId, String artifactId) {

    return new DefaultArtifact(groupId, artifactId, "1.0", "compile", "jar", null, new DefaultArtifactHandler("jar"));
  }

  private static Exclusion exclusion (String groupId, String artifactId) {

    Exclusion exclusion = new Exclusion();

    exclusion.setGroupId(groupId);
    exclusion.setArtifactId(artifactId);

    return exclusion;
  }

  public void testMatchesOnlyWhenBothCoordinatesAgree () {

    Exclusion exclusion = exclusion("org.smallmind", "smallmind-nutsnbolts");

    Assert.assertTrue(exclusion.matchesArtifact(artifact("org.smallmind", "smallmind-nutsnbolts")));
  }

  public void testDifferentGroupDoesNotMatch () {

    Exclusion exclusion = exclusion("org.smallmind", "smallmind-nutsnbolts");

    Assert.assertFalse(exclusion.matchesArtifact(artifact("org.other", "smallmind-nutsnbolts")));
  }

  public void testDifferentArtifactDoesNotMatch () {

    Exclusion exclusion = exclusion("org.smallmind", "smallmind-nutsnbolts");

    Assert.assertFalse(exclusion.matchesArtifact(artifact("org.smallmind", "smallmind-scribe")));
  }
}
