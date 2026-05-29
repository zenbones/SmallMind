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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class Base64CodecTest {

  public void testStringEncodeMatchesRfc4648FixtureForEmpty ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode(""), "");
  }

  public void testStringEncodeMatchesRfc4648FixtureForSingleByte ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode("f"), "Zg==");
  }

  public void testStringEncodeMatchesRfc4648FixtureForTwoBytes ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode("fo"), "Zm8=");
  }

  public void testStringEncodeMatchesRfc4648FixtureForThreeBytes ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode("foo"), "Zm9v");
  }

  public void testStringEncodeMatchesRfc4648FixtureForFourBytes ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode("foob"), "Zm9vYg==");
  }

  public void testStringEncodeWithoutPaddingStripsTrailingEquals ()
    throws IOException {

    Assert.assertEquals(Base64Codec.encode("f", false), "Zg");
    Assert.assertEquals(Base64Codec.encode("fo", false), "Zm8");
    Assert.assertEquals(Base64Codec.encode("foo", false), "Zm9v");
  }

  public void testStandardDecodeReversesStandardEncode ()
    throws IOException {

    String original = "The quick brown fox jumps over 13 lazy dogs!";

    Assert.assertEquals(new String(Base64Codec.decode(Base64Codec.encode(original)), StandardCharsets.UTF_8), original);
  }

  public void testUrlSafeRoundTripReplacesPlusAndSlashAndOmitsPadding ()
    throws IOException {

    byte[] bytes = {-1, -2, -3, -4, -5, -6, -7};
    String urlSafe = Base64Codec.urlSafeEncode(bytes);

    Assert.assertFalse(urlSafe.contains("+"));
    Assert.assertFalse(urlSafe.contains("/"));
    Assert.assertFalse(urlSafe.contains("="));
    Assert.assertEquals(Base64Codec.urlSafeDecode(urlSafe), bytes);
  }

  public void testCustomAlphabetRoundTripUsesProvidedChars ()
    throws IOException {

    byte[] bytes = {-1, -2, 5, 6, 7};
    String encoded = Base64Codec.encode(bytes, false, '*', '@');

    Assert.assertFalse(encoded.contains("+"));
    Assert.assertFalse(encoded.contains("/"));
    Assert.assertEquals(Base64Codec.decode(encoded.getBytes(StandardCharsets.UTF_8), false, '*', '@'), bytes);
  }

  public void testBinaryRoundTripPreservesArbitraryBytes ()
    throws IOException {

    byte[] bytes = new byte[256];

    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte)i;
    }

    Assert.assertEquals(Base64Codec.decode(Base64Codec.encode(bytes)), bytes);
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testStrictDecodeRejectsTruncatedInput ()
    throws IOException {

    Base64Codec.decode("abc", true);
  }

  public void testByteBufferRoundTripStandardAndUrlSafe ()
    throws IOException {

    byte[] bytes = "hello world!".getBytes(StandardCharsets.UTF_8);

    String standard = Base64Codec.encode(ByteBuffer.wrap(bytes));
    Assert.assertEquals(Base64Codec.decode(ByteBuffer.wrap(standard.getBytes(StandardCharsets.UTF_8))), bytes);

    String unpadded = Base64Codec.encode(ByteBuffer.wrap(bytes), false);
    Assert.assertFalse(unpadded.endsWith("="));

    String urlSafe = Base64Codec.urlSafeEncode(ByteBuffer.wrap(bytes));
    Assert.assertFalse(urlSafe.contains("+"));
    Assert.assertFalse(urlSafe.contains("/"));
  }

  public void testByteBufferEncodeAcceptsCustomAlphabet ()
    throws IOException {

    byte[] bytes = {-1, -2, 5, 6, 7};

    String custom = Base64Codec.encode(ByteBuffer.wrap(bytes), '*', '@');
    Assert.assertFalse(custom.contains("+"));
    Assert.assertEquals(Base64Codec.decode(custom.getBytes(StandardCharsets.UTF_8), true, '*', '@'), bytes);

    String unpadded = Base64Codec.encode(ByteBuffer.wrap(bytes), false, '*', '@');
    Assert.assertFalse(unpadded.endsWith("="));
  }

  public void testInputStreamRoundTripStandardAndUrlSafe ()
    throws IOException {

    byte[] bytes = "hello world!".getBytes(StandardCharsets.UTF_8);

    String standard = Base64Codec.encode(new ByteArrayInputStream(bytes));
    Assert.assertEquals(Base64Codec.decode(new ByteArrayInputStream(standard.getBytes(StandardCharsets.UTF_8))), bytes);

    String unpadded = Base64Codec.encode(new ByteArrayInputStream(bytes), false);
    Assert.assertFalse(unpadded.endsWith("="));

    String urlSafe = Base64Codec.urlSafeEncode(new ByteArrayInputStream(bytes));
    Assert.assertEquals(Base64Codec.urlSafeDecode(new ByteArrayInputStream(urlSafe.getBytes(StandardCharsets.UTF_8))), bytes);
  }

  public void testInputStreamEncodeAcceptsCustomAlphabet ()
    throws IOException {

    byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    String padded = Base64Codec.encode(new ByteArrayInputStream(bytes), '*', '@');
    Assert.assertEquals(Base64Codec.decode(padded.getBytes(StandardCharsets.UTF_8), true, '*', '@'), bytes);

    String unpadded = Base64Codec.encode(new ByteArrayInputStream(bytes), false, '*', '@');
    Assert.assertFalse(unpadded.endsWith("="));
    Assert.assertEquals(Base64Codec.decode(unpadded.getBytes(StandardCharsets.UTF_8), false, '*', '@'), bytes);
  }

  public void testStringEncodeAcceptsCustomAlphabet ()
    throws IOException {

    String encoded = Base64Codec.encode("foob", '*', '@');
    Assert.assertEquals(Base64Codec.decode(encoded.getBytes(StandardCharsets.UTF_8), true, '*', '@'), "foob".getBytes(StandardCharsets.UTF_8));

    String unpadded = Base64Codec.encode("foob", false, '*', '@');
    Assert.assertFalse(unpadded.endsWith("="));
  }

  public void testUrlSafeDecodeOverloads ()
    throws IOException {

    byte[] bytes = {10, 20, 30, 40, 50, 60, 70};
    String urlSafe = Base64Codec.urlSafeEncode(bytes);

    Assert.assertEquals(Base64Codec.urlSafeDecode(urlSafe.getBytes(StandardCharsets.UTF_8)), bytes);
    Assert.assertEquals(Base64Codec.urlSafeDecode(new ByteArrayInputStream(urlSafe.getBytes(StandardCharsets.UTF_8))), bytes);
  }

  public void testByteArrayDecodeOverloads ()
    throws IOException {

    byte[] bytes = "round-trip".getBytes(StandardCharsets.UTF_8);
    String standard = Base64Codec.encode(bytes);

    Assert.assertEquals(Base64Codec.decode(standard.getBytes(StandardCharsets.UTF_8)), bytes);
    Assert.assertEquals(Base64Codec.decode(standard.getBytes(StandardCharsets.UTF_8), true), bytes);
    Assert.assertEquals(Base64Codec.decode(standard.getBytes(StandardCharsets.UTF_8), true, '+', '/'), bytes);

    String customUnpadded = Base64Codec.encode(bytes, false, '*', '@');
    Assert.assertEquals(Base64Codec.decode(customUnpadded.getBytes(StandardCharsets.UTF_8), false, '*', '@'), bytes);
  }

  public void testStringDecodeWithStrictFlagAcceptsExactBlocks ()
    throws IOException {

    Assert.assertEquals(Base64Codec.decode("Zm9v", true), "foo".getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(Base64Codec.decode("Zm9v", false), "foo".getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(Base64Codec.decode("Zm9v", '+', '/'), "foo".getBytes(StandardCharsets.UTF_8));
  }

  public void testByteBufferDecodeOverloads ()
    throws IOException {

    byte[] bytes = "buffer".getBytes(StandardCharsets.UTF_8);
    String standard = Base64Codec.encode(bytes);
    ByteBuffer buffer = ByteBuffer.wrap(standard.getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(Base64Codec.decode(buffer.duplicate()), bytes);
    Assert.assertEquals(Base64Codec.decode(buffer.duplicate(), true), bytes);
    Assert.assertEquals(Base64Codec.decode(buffer.duplicate(), true, '+', '/'), bytes);

    String customUnpadded = Base64Codec.encode(bytes, false, '*', '@');
    Assert.assertEquals(Base64Codec.decode(ByteBuffer.wrap(customUnpadded.getBytes(StandardCharsets.UTF_8)), false, '*', '@'), bytes);
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testDecodeRejectsInvalidCharacter ()
    throws IOException {

    Base64Codec.decode("Zm9!");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testDecodeRejectsMisplacedPadding ()
    throws IOException {

    Base64Codec.decode("=m9v");
  }
}
