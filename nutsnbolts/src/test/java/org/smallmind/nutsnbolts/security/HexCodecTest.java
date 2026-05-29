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
package org.smallmind.nutsnbolts.security;

import java.io.UnsupportedEncodingException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HexCodecTest {

  public void testEncodeIsLowercase () {

    Assert.assertEquals(HexCodec.hexEncode(new byte[]{(byte)0xAB, (byte)0xCD}), "abcd");
  }

  public void testEncodeZeroPadsByteValuesBelow16 () {

    Assert.assertEquals(HexCodec.hexEncode(new byte[]{0x00, 0x01, 0x0F}), "00010f");
  }

  public void testEncodeHandlesNegativeBytesAsUnsigned () {

    Assert.assertEquals(HexCodec.hexEncode(new byte[]{(byte)0xFF, (byte)0x80}), "ff80");
  }

  public void testEncodeSliceUsesOffsetAndLength () {

    Assert.assertEquals(HexCodec.hexEncode(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}, 1, 3), "020304");
  }

  public void testRoundTripPreservesAllByteValues ()
    throws UnsupportedEncodingException {

    byte[] original = new byte[256];

    for (int i = 0; i < 256; i++) {
      original[i] = (byte)i;
    }

    Assert.assertEquals(HexCodec.hexDecode(HexCodec.hexEncode(original)), original);
  }

  public void testDecodeAcceptsBothCases ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(HexCodec.hexDecode("AbCdEf"), new byte[]{(byte)0xAB, (byte)0xCD, (byte)0xEF});
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testDecodeRejectsOddLength ()
    throws UnsupportedEncodingException {

    HexCodec.hexDecode("abc");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testDecodeRejectsNonHexCharacter ()
    throws UnsupportedEncodingException {

    HexCodec.hexDecode("ab!d");
  }
}
