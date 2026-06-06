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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ByteArrayIOStreamTest {

  public void testWriteThenReadRoundTripsBytes ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write('a');
    stream.asOutputStream().write('b');
    stream.asOutputStream().write('c');

    Assert.assertEquals(stream.size(), 3);
    Assert.assertEquals(stream.asInputStream().read(), 'a');
    Assert.assertEquals(stream.asInputStream().read(), 'b');
    Assert.assertEquals(stream.asInputStream().read(), 'c');
    Assert.assertEquals(stream.asInputStream().read(), -1);
  }

  public void testAvailableReflectsUnreadBytes ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (int i = 0; i < 5; i++) {
      stream.asOutputStream().write(i);
    }

    Assert.assertEquals(stream.asInputStream().available(), 5);
    stream.asInputStream().read();
    Assert.assertEquals(stream.asInputStream().available(), 4);
  }

  public void testClearResetsContentAndSize ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write('a');
    stream.asOutputStream().write('b');
    stream.clear();

    Assert.assertEquals(stream.size(), 0);
    Assert.assertEquals(stream.asInputStream().read(), -1);
  }

  public void testTruncateShortensStream ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (int i = 0; i < 10; i++) {
      stream.asOutputStream().write(i);
    }

    stream.truncate(3);
    Assert.assertEquals(stream.size(), 3);
  }

  public void testCloseFlagIsSet ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    Assert.assertFalse(stream.isClosed());

    stream.close();
    Assert.assertTrue(stream.isClosed());
  }

  @Test(expectedExceptions = IOException.class)
  public void testSizeAfterCloseThrows ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.close();
    stream.size();
  }

  public void testMarkAndResetRestoreReadPosition ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (int i = 0; i < 5; i++) {
      stream.asOutputStream().write(i);
    }

    Assert.assertTrue(stream.asInputStream().markSupported());

    stream.asInputStream().read();
    stream.asInputStream().mark(100);
    stream.asInputStream().read();
    stream.asInputStream().read();
    stream.asInputStream().reset();

    Assert.assertEquals(stream.asInputStream().read(), 1);
  }

  public void testRewindOnInputResetsToBeginning ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (int i = 0; i < 3; i++) {
      stream.asOutputStream().write(i);
    }

    stream.asInputStream().read();
    stream.asInputStream().read();
    stream.asInputStream().rewind();

    Assert.assertEquals(stream.asInputStream().read(), 0);
  }

  public void testPeekDoesNotConsume ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write('a');
    stream.asOutputStream().write('b');
    stream.asOutputStream().write('c');

    Assert.assertEquals(stream.asInputStream().peek(0), (byte)'a');
    Assert.assertEquals(stream.asInputStream().peek(2), (byte)'c');
    Assert.assertEquals(stream.asInputStream().read(), 'a');
  }

  public void testPositionGetterAndSetter ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (int i = 0; i < 5; i++) {
      stream.asOutputStream().write(i);
    }

    Assert.assertEquals(stream.asInputStream().position(), 0);

    stream.asInputStream().position(3);
    Assert.assertEquals(stream.asInputStream().position(), 3);
    Assert.assertEquals(stream.asInputStream().read(), 3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeReadPositionIsRejected ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asInputStream().position(-1);
  }

  public void testReadAvailableReturnsRemainingBytes ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write('x');
    stream.asOutputStream().write('y');
    stream.asOutputStream().write('z');

    Assert.assertEquals(stream.asInputStream().readAvailable(), new byte[] {'x', 'y', 'z'});
  }

  public void testToStringReturnsBufferedContentAsUtf8 ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    for (byte b : "hello".getBytes()) {
      stream.asOutputStream().write(b);
    }

    Assert.assertEquals(stream.toString(), "hello");
  }

  public void testArrayWriteFromZeroOffsetCopiesAllBytes ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write(new byte[] {1, 2, 3, 4, 5}, 0, 5);

    Assert.assertEquals(stream.size(), 5);
    Assert.assertEquals(stream.asInputStream().readAvailable(), new byte[] {1, 2, 3, 4, 5});
  }

  public void testArrayWriteFromNonZeroOffsetCopiesCorrectSlice ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write(new byte[] {1, 2, 3, 4, 5}, 1, 3);

    Assert.assertEquals(stream.size(), 3);
    Assert.assertEquals(stream.asInputStream().readAvailable(), new byte[] {2, 3, 4});
  }

  public void testArrayWriteOfTrailingSliceCopiesCorrectLength ()
    throws IOException {

    ByteArrayIOStream stream = new ByteArrayIOStream();

    stream.asOutputStream().write(new byte[] {1, 2, 3, 4, 5}, 3, 2);

    Assert.assertEquals(stream.size(), 2);
    Assert.assertEquals(stream.asInputStream().readAvailable(), new byte[] {4, 5});
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testArrayWriteWithOffsetPlusLengthBeyondInputIsRejected ()
    throws IOException {

    new ByteArrayIOStream().asOutputStream().write(new byte[] {1, 2, 3}, 1, 5);
  }
}
