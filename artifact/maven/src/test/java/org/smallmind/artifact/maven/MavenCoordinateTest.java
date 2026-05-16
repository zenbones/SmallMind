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

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MavenCoordinateTest {

  public void testThreeArgConstructorDefaultsExtensionToJar () {

    MavenCoordinate coordinate = new MavenCoordinate("org.example", "lib", "1.0.0");

    Assert.assertEquals(coordinate.getGroupId(), "org.example");
    Assert.assertEquals(coordinate.getArtifactId(), "lib");
    Assert.assertEquals(coordinate.getVersion(), "1.0.0");
    Assert.assertEquals(coordinate.getExtension(), "jar");
    Assert.assertNull(coordinate.getClassifier());
  }

  public void testFourArgConstructorAcceptsClassifier () {

    MavenCoordinate coordinate = new MavenCoordinate("org.example", "lib", "sources", "1.0.0");

    Assert.assertEquals(coordinate.getClassifier(), "sources");
    Assert.assertEquals(coordinate.getExtension(), "jar");
  }

  public void testFiveArgConstructorPopulatesAllFields () {

    MavenCoordinate coordinate = new MavenCoordinate("org.example", "lib", "tests", "war", "2.0.0");

    Assert.assertEquals(coordinate.getGroupId(), "org.example");
    Assert.assertEquals(coordinate.getArtifactId(), "lib");
    Assert.assertEquals(coordinate.getClassifier(), "tests");
    Assert.assertEquals(coordinate.getExtension(), "war");
    Assert.assertEquals(coordinate.getVersion(), "2.0.0");
  }

  public void testSettersUpdateFields () {

    MavenCoordinate coordinate = new MavenCoordinate();

    coordinate.setGroupId("org.example");
    coordinate.setArtifactId("lib");
    coordinate.setClassifier("javadoc");
    coordinate.setExtension("zip");
    coordinate.setVersion("3.0.0");

    Assert.assertEquals(coordinate.getGroupId(), "org.example");
    Assert.assertEquals(coordinate.getArtifactId(), "lib");
    Assert.assertEquals(coordinate.getClassifier(), "javadoc");
    Assert.assertEquals(coordinate.getExtension(), "zip");
    Assert.assertEquals(coordinate.getVersion(), "3.0.0");
  }

  public void testEqualsForIdenticalCoordinates () {

    MavenCoordinate a = new MavenCoordinate("org.example", "lib", "tests", "war", "1.0.0");
    MavenCoordinate b = new MavenCoordinate("org.example", "lib", "tests", "war", "1.0.0");

    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testEqualsIsReflexive () {

    MavenCoordinate coordinate = new MavenCoordinate("org.example", "lib", "1.0.0");

    Assert.assertEquals(coordinate, coordinate);
  }

  public void testEqualsDistinguishesNullClassifierFromPresent () {

    MavenCoordinate plain = new MavenCoordinate("org.example", "lib", "1.0.0");
    MavenCoordinate sources = new MavenCoordinate("org.example", "lib", "sources", "1.0.0");

    Assert.assertNotEquals(plain, sources);
    Assert.assertNotEquals(sources, plain);
  }

  public void testEqualsTreatsBothNullClassifiersAsEqual () {

    MavenCoordinate a = new MavenCoordinate("org.example", "lib", "1.0.0");
    MavenCoordinate b = new MavenCoordinate("org.example", "lib", "1.0.0");

    Assert.assertNull(a.getClassifier());
    Assert.assertNull(b.getClassifier());
    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testEqualsDistinguishesVersion () {

    MavenCoordinate a = new MavenCoordinate("org.example", "lib", "1.0.0");
    MavenCoordinate b = new MavenCoordinate("org.example", "lib", "2.0.0");

    Assert.assertNotEquals(a, b);
  }

  public void testEqualsRejectsNonCoordinate () {

    MavenCoordinate coordinate = new MavenCoordinate("org.example", "lib", "1.0.0");

    Assert.assertNotEquals(coordinate, "org.example:lib:1.0.0");
  }
}
