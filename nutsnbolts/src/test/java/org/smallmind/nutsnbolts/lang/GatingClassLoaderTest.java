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
package org.smallmind.nutsnbolts.lang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GatingClassLoaderTest {

  @SuppressWarnings("unused")
  private static String unused = "marker-" + new String("x".getBytes(StandardCharsets.UTF_8));

  public void testParentDelegationLoadsKnownClass ()
    throws ClassNotFoundException {

    ClassLoader parent = getClass().getClassLoader();
    GatingClassLoader loader = new GatingClassLoader(parent, -1, new ClasspathClassGate());

    Class<?> resolved = loader.loadClass("java.lang.String");

    Assert.assertEquals(resolved, String.class);
  }

  public void testFindResourceReturnsNullForMissingPath ()
    throws IOException {

    GatingClassLoader loader = new GatingClassLoader(null, -1, new ClasspathClassGate());

    Assert.assertNull(loader.findResource("no/such/resource.bin"));
  }

  public void testGetResourceAsStreamFallsBackToGates ()
    throws IOException {

    ClassLoader parent = getClass().getClassLoader();
    GatingClassLoader loader = new GatingClassLoader(parent, -1, new ClasspathClassGate());

    try (InputStream stream = loader.getResourceAsStream("org/smallmind/nutsnbolts/lang/GatingClassLoader.class")) {
      Assert.assertNotNull(stream);
      Assert.assertTrue(stream.read() != -1);
    }
  }

  public void testGetClassGatesReturnsConfiguredGates () {

    ClasspathClassGate first = new ClasspathClassGate();
    ClasspathClassGate second = new ClasspathClassGate(".");
    GatingClassLoader loader = new GatingClassLoader(null, -1, first, second);

    ClassGate[] gates = loader.getClassGates();

    Assert.assertEquals(gates.length, 2);
    Assert.assertSame(gates[0], first);
    Assert.assertSame(gates[1], second);
  }

  public void testClasspathClassGateLocatesKnownResourceOnClasspath ()
    throws IOException {

    ClasspathClassGate gate = new ClasspathClassGate(System.getProperty("java.class.path"));

    try (InputStream stream = gate.getResourceAsStream("org/smallmind/nutsnbolts/lang/GatingClassLoader.class")) {
      Assert.assertNotNull(stream, "Expected the class to be locatable through the gate");
      byte[] bytes = stream.readNBytes(4);
      Assert.assertEquals(bytes[0] & 0xff, 0xCA);
      Assert.assertEquals(bytes[1] & 0xff, 0xFE);
      Assert.assertEquals(bytes[2] & 0xff, 0xBA);
      Assert.assertEquals(bytes[3] & 0xff, 0xBE);
    }
  }
}
