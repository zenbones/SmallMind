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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MarkableInputStreamEdgesTest {

  public void testAvailableWithoutMarkDelegatesToUnderlying ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    Assert.assertEquals(stream.available(), payload.length);
  }

  public void testAvailableAfterReadAndResetIncludesBufferRemainder ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(6);
    stream.read(new byte[3]);
    stream.reset();

    Assert.assertEquals(stream.available(), 6);
  }

  public void testReadingMoreThanReadLimitInvalidatesBuffer ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(2);
    stream.read(new byte[3]);

    try {
      stream.reset();
      Assert.fail("Expected reset to throw because buffer was invalidated");
    } catch (IOException expected) {
      Assert.assertTrue(expected.getMessage().contains("mark") || expected.getMessage().contains("Mark"));
    }
  }

  public void testMarkAfterPartialReadKeepsUnreadBufferedBytes ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(6);
    byte[] first = stream.readNBytes(3);
    Assert.assertEquals(new String(first, StandardCharsets.UTF_8), "abc");

    stream.reset();
    byte[] second = stream.readNBytes(3);
    Assert.assertEquals(new String(second, StandardCharsets.UTF_8), "abc");

    stream.mark(6);
    stream.reset();
    byte[] third = stream.readNBytes(3);
    Assert.assertEquals(new String(third, StandardCharsets.UTF_8), "def");
  }

  public void testSkipWhileMarkActiveCapturesIntoBuffer ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(6);
    long skipped = stream.skip(2);

    Assert.assertEquals(skipped, 2);

    stream.reset();
    byte[] all = stream.readNBytes(6);
    Assert.assertEquals(new String(all, StandardCharsets.UTF_8), "abcdef");
  }

  public void testSkipBeyondBufferFallsBackToUnderlying ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(2);
    stream.skip(4);

    try {
      stream.reset();
      Assert.fail("Expected reset to throw after skip beyond buffer");
    } catch (IOException expected) {

    }
  }

  public void testReadZeroLengthReturnsZero ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    Assert.assertEquals(stream.read(new byte[3], 0, 0), 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testReadWithNullBufferThrows ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    stream.read(null, 0, 1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testReadWithBadOffsetThrows ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    stream.read(new byte[3], -1, 1);
  }

  @Test(expectedExceptions = IOException.class)
  public void testAvailableAfterCloseThrows ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1}));

    stream.close();
    stream.available();
  }
}
