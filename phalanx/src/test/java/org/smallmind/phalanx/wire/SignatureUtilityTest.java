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
package org.smallmind.phalanx.wire;

import java.time.LocalDateTime;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Locks the wire-level signature contract produced by {@link SignatureUtility}: the neutral
 * (cross-JVM) encoding, the JVM descriptor encoding, and the descriptor-to-class round trip.
 * These strings are part of the on-the-wire protocol, so any drift here is a protocol break.
 */
@Test(groups = "unit")
public class SignatureUtilityTest {

  @Test
  public void testNeutralEncodePrimitives () {

    Assert.assertEquals(SignatureUtility.neutralEncode(boolean.class), "Z");
    Assert.assertEquals(SignatureUtility.neutralEncode(byte.class), "B");
    Assert.assertEquals(SignatureUtility.neutralEncode(short.class), "S");
    Assert.assertEquals(SignatureUtility.neutralEncode(int.class), "I");
    Assert.assertEquals(SignatureUtility.neutralEncode(long.class), "L");
    Assert.assertEquals(SignatureUtility.neutralEncode(float.class), "F");
    Assert.assertEquals(SignatureUtility.neutralEncode(double.class), "D");
    Assert.assertEquals(SignatureUtility.neutralEncode(char.class), "C");
  }

  @Test
  public void testNeutralEncodeWellKnownReferenceTypes () {

    Assert.assertEquals(SignatureUtility.neutralEncode(String.class), "G");
    Assert.assertEquals(SignatureUtility.neutralEncode(LocalDateTime.class), "T");
    Assert.assertEquals(SignatureUtility.neutralEncode(Fault.class), "A");
    Assert.assertEquals(SignatureUtility.neutralEncode(Object.class), "O");
  }

  @Test
  public void testNeutralEncodeUnknownReferenceTypeUsesSimpleName () {

    Assert.assertEquals(SignatureUtility.neutralEncode(Color.class), "!Color");
  }

  @Test
  public void testNeutralEncodeVoidAndNull () {

    Assert.assertEquals(SignatureUtility.neutralEncode(null), "V");
    Assert.assertEquals(SignatureUtility.neutralEncode(void.class), "V");
    Assert.assertEquals(SignatureUtility.neutralEncode(Void.class), "V");
  }

  @Test
  public void testNeutralEncodeArraysCarryDimensionsAsCommas () {

    Assert.assertEquals(SignatureUtility.neutralEncode(int[].class), "I[]");
    Assert.assertEquals(SignatureUtility.neutralEncode(int[][].class), "I[,]");
    Assert.assertEquals(SignatureUtility.neutralEncode(String[].class), "G[]");
    Assert.assertEquals(SignatureUtility.neutralEncode(Color[].class), "!Color[]");
  }

  @Test
  public void testNativeEncode () {

    Assert.assertEquals(SignatureUtility.nativeEncode(int.class), "I");
    Assert.assertEquals(SignatureUtility.nativeEncode(long.class), "J");
    Assert.assertEquals(SignatureUtility.nativeEncode(boolean.class), "Z");
    Assert.assertEquals(SignatureUtility.nativeEncode(String.class), "Ljava/lang/String;");
    Assert.assertEquals(SignatureUtility.nativeEncode(int[].class), "[I");
    Assert.assertEquals(SignatureUtility.nativeEncode(String[].class), "[Ljava/lang/String;");
    Assert.assertEquals(SignatureUtility.nativeEncode(null), "V");
    Assert.assertEquals(SignatureUtility.nativeEncode(void.class), "V");
  }

  @Test
  public void testNativeDecode ()
    throws ClassNotFoundException {

    Assert.assertEquals(SignatureUtility.nativeDecode("I"), int.class);
    Assert.assertEquals(SignatureUtility.nativeDecode("J"), long.class);
    Assert.assertEquals(SignatureUtility.nativeDecode("Z"), boolean.class);
    Assert.assertEquals(SignatureUtility.nativeDecode("C"), char.class);
    Assert.assertEquals(SignatureUtility.nativeDecode("Ljava/lang/String;"), String.class);
    Assert.assertEquals(SignatureUtility.nativeDecode("[I"), int[].class);
  }

  @Test
  public void testNativeEncodeDecodeRoundTrip ()
    throws ClassNotFoundException {

    Assert.assertEquals(SignatureUtility.nativeDecode(SignatureUtility.nativeEncode(int.class)), int.class);
    Assert.assertEquals(SignatureUtility.nativeDecode(SignatureUtility.nativeEncode(long.class)), long.class);
    Assert.assertEquals(SignatureUtility.nativeDecode(SignatureUtility.nativeEncode(String.class)), String.class);
    Assert.assertEquals(SignatureUtility.nativeDecode(SignatureUtility.nativeEncode(int[].class)), int[].class);
    Assert.assertEquals(SignatureUtility.nativeDecode(SignatureUtility.nativeEncode(String[].class)), String[].class);
  }

  @Test(expectedExceptions = ClassNotFoundException.class)
  public void testNativeDecodeRejectsUnknownTypeCode ()
    throws ClassNotFoundException {

    SignatureUtility.nativeDecode("Q");
  }
}
