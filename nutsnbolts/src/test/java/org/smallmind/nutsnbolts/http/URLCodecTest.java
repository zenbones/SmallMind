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
package org.smallmind.nutsnbolts.http;

import java.io.UnsupportedEncodingException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class URLCodecTest {

  public void testUnreservedCharactersPassThroughUnchanged ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("abcXYZ-_.~0123"), "abcXYZ-_.~0123");
  }

  public void testSpaceEncodesAsPlusByDefault ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("a b c"), "a+b+c");
  }

  public void testSpaceEncodesAsPercent20WhenPlusDisabled ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("a b", false), "a%20b");
  }

  public void testReservedAsciiCharactersAreEscaped ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("a/b?c=d&e"), "a%2Fb%3Fc%3Dd%26e");
  }

  public void testTwoByteUtf8CharacterEncodesAsTwoEscapes ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("\u00E9"), "%C3%A9");
  }

  public void testThreeByteUtf8CharacterEncodesAsThreeEscapes ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode("\u20AC"), "%E2%82%AC");
  }

  public void testSurrogatePairEncodesAsFourEscapes ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlEncode(new String(Character.toChars(0x1F600))), "%F0%9F%98%80");
  }

  public void testDecodePlusReturnsSpace ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlDecode("a+b+c"), "a b c");
  }

  public void testDecodePercentEscapeReturnsAsciiCharacter ()
    throws UnsupportedEncodingException {

    Assert.assertEquals(URLCodec.urlDecode("a%2Fb"), "a/b");
  }

  public void testEncodeDecodeRoundTripPreservesUnicode ()
    throws UnsupportedEncodingException {

    String original = "Hello, world! Caf\u00E9 / 100% \u20AC " + new String(Character.toChars(0x1F600));

    Assert.assertEquals(URLCodec.urlDecode(URLCodec.urlEncode(original)), original);
  }

  public void testRoundTripAcrossMultipleScripts ()
    throws UnsupportedEncodingException {

    String[] samples = {
      "Caf\u00E9 na\u00EFve r\u00E9sum\u00E9",
      "\u041F\u0440\u0438\u0432\u0435\u0442 \u043C\u0438\u0440",
      "\u0393\u03B5\u03B9\u03AC \u03C3\u03BF\u03C5 \u03BA\u03CC\u03C3\u03BC\u03B5",
      "\u05E9\u05DC\u05D5\u05DD \u05E2\u05D5\u05DC\u05DD",
      "\u0645\u0631\u062D\u0628\u0627 \u0628\u0627\u0644\u0639\u0627\u0644\u0645",
      "\u4F60\u597D\u4E16\u754C",
      "\u3053\u3093\u306B\u3061\u306F\u4E16\u754C",
      "\uC548\uB155\uD558\uC138\uC694 \uC138\uACC4",
      "\u0E2A\u0E27\u0E31\u0E2A\u0E14\u0E35\u0E0A\u0E32\u0E27\u0E42\u0E25\u0E01",
      "\u0928\u092E\u0938\u094D\u0924\u0947 \u0926\u0941\u0928\u093F\u092F\u093E",
      new String(Character.toChars(0x1F600)) + new String(Character.toChars(0x1F30D)),
      new String(Character.toChars(0x20000)) + new String(Character.toChars(0x2A6DF))
    };

    for (String sample : samples) {
      Assert.assertEquals(URLCodec.urlDecode(URLCodec.urlEncode(sample)), sample, "round-trip failed for: " + sample);
    }
  }

  public void testStringWithoutChangesIsReturnedAsIs ()
    throws UnsupportedEncodingException {

    String original = "noChangesNeeded";

    Assert.assertSame(URLCodec.urlEncode(original), original);
    Assert.assertSame(URLCodec.urlDecode(original), original);
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testTruncatedPercentEscapeIsRejected ()
    throws UnsupportedEncodingException {

    URLCodec.urlDecode("abc%2");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testInvalidHexDigitInEscapeIsRejected ()
    throws UnsupportedEncodingException {

    URLCodec.urlDecode("a%ZZb");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testUnpairedHighSurrogateIsRejected ()
    throws UnsupportedEncodingException {

    URLCodec.urlEncode("\uD83D");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testUnpairedLowSurrogateIsRejected ()
    throws UnsupportedEncodingException {

    URLCodec.urlEncode("\uDE00");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testHighSurrogateFollowedByNonLowIsRejected ()
    throws UnsupportedEncodingException {

    URLCodec.urlEncode("\uD83D" + "a");
  }
}
