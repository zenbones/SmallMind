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
package org.smallmind.nutsnbolts.util;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BytesTest {

  public void testLongEncodesToEightBytesBigEndian () {

    byte[] bytes = Bytes.getBytes(0x0102030405060708L);

    Assert.assertEquals(bytes.length, 8);
    Assert.assertEquals(bytes[0], (byte)0x01);
    Assert.assertEquals(bytes[7], (byte)0x08);
  }

  public void testIntEncodesToFourBytesBigEndian () {

    byte[] bytes = Bytes.getBytes(0x01020304);

    Assert.assertEquals(bytes.length, 4);
    Assert.assertEquals(bytes[0], (byte)0x01);
    Assert.assertEquals(bytes[3], (byte)0x04);
  }

  public void testShortEncodesToTwoBytesBigEndian () {

    byte[] bytes = Bytes.getBytes((short)0x0102);

    Assert.assertEquals(bytes.length, 2);
    Assert.assertEquals(bytes[0], (byte)0x01);
    Assert.assertEquals(bytes[1], (byte)0x02);
  }

  public void testLongRoundTrip () {

    Assert.assertEquals(Bytes.getLong(Bytes.getBytes(Long.MAX_VALUE)), Long.MAX_VALUE);
    Assert.assertEquals(Bytes.getLong(Bytes.getBytes(Long.MIN_VALUE)), Long.MIN_VALUE);
    Assert.assertEquals(Bytes.getLong(Bytes.getBytes(0L)), 0L);
    Assert.assertEquals(Bytes.getLong(Bytes.getBytes(-1L)), -1L);
  }

  public void testIntRoundTrip () {

    Assert.assertEquals(Bytes.getInt(Bytes.getBytes(Integer.MAX_VALUE)), Integer.MAX_VALUE);
    Assert.assertEquals(Bytes.getInt(Bytes.getBytes(Integer.MIN_VALUE)), Integer.MIN_VALUE);
    Assert.assertEquals(Bytes.getInt(Bytes.getBytes(0)), 0);
    Assert.assertEquals(Bytes.getInt(Bytes.getBytes(-1)), -1);
  }

  public void testShortRoundTrip () {

    Assert.assertEquals(Bytes.getShort(Bytes.getBytes(Short.MAX_VALUE)), Short.MAX_VALUE);
    Assert.assertEquals(Bytes.getShort(Bytes.getBytes(Short.MIN_VALUE)), Short.MIN_VALUE);
    Assert.assertEquals(Bytes.getShort(Bytes.getBytes((short)0)), (short)0);
  }
}
