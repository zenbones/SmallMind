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
package org.smallmind.memcached.cubby.response;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.connection.ExposedByteArrayOutputStream;
import org.smallmind.nutsnbolts.lang.FormattedIllegalArgumentException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JoinedBufferTest {

  private static JoinedBuffer buffer (String accumulated, String fresh) {

    ExposedByteArrayOutputStream accumulator = new ExposedByteArrayOutputStream(Math.max(accumulated.length(), 1));

    if (!accumulated.isEmpty()) {
      accumulator.writeBytes(accumulated.getBytes(StandardCharsets.UTF_8));
    }

    return new JoinedBuffer(accumulator, ByteBuffer.wrap(fresh.getBytes(StandardCharsets.UTF_8)));
  }

  public void testLimitIsSumOfAccumulatedAndFreshBuffers () {

    JoinedBuffer joinedBuffer = buffer("HEAD", "TAIL");

    Assert.assertEquals(joinedBuffer.limit(), 8);
    Assert.assertEquals(joinedBuffer.remaining(), 8);
    Assert.assertEquals(joinedBuffer.position(), 0);
  }

  public void testSequentialGetDrainsAccumulatedThenFreshBuffer () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    Assert.assertEquals(joinedBuffer.get(), (byte)'A');
    Assert.assertEquals(joinedBuffer.get(), (byte)'B');
    Assert.assertEquals(joinedBuffer.get(), (byte)'C');
    Assert.assertEquals(joinedBuffer.get(), (byte)'D');
    Assert.assertEquals(joinedBuffer.position(), 4);
  }

  public void testAbsoluteGetReturnsByteAtIndexFromEitherBuffer () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    Assert.assertEquals(joinedBuffer.get(0), (byte)'A');
    Assert.assertEquals(joinedBuffer.get(1), (byte)'B');
    Assert.assertEquals(joinedBuffer.get(2), (byte)'C');
    Assert.assertEquals(joinedBuffer.get(3), (byte)'D');
    Assert.assertEquals(joinedBuffer.position(), 0);
  }

  public void testBulkGetCopiesBytesAcrossBoundary () {

    JoinedBuffer joinedBuffer = buffer("AB", "CDE");

    byte[] bytes = joinedBuffer.get(new byte[5]);

    Assert.assertEquals(new String(bytes, StandardCharsets.UTF_8), "ABCDE");
    Assert.assertEquals(joinedBuffer.position(), 5);
  }

  public void testPeekDoesNotAdvancePosition () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    joinedBuffer.get();
    Assert.assertEquals(joinedBuffer.position(), 1);
    Assert.assertEquals(joinedBuffer.peek(0), (byte)'B');
    Assert.assertEquals(joinedBuffer.peek(1), (byte)'C');
    Assert.assertEquals(joinedBuffer.position(), 1);
  }

  public void testMarkAndResetRestorePositionAcrossBoundary () {

    JoinedBuffer joinedBuffer = buffer("AB", "CDEF");

    joinedBuffer.get();
    joinedBuffer.get();
    joinedBuffer.mark();
    joinedBuffer.get();
    joinedBuffer.get();
    Assert.assertEquals(joinedBuffer.position(), 4);

    joinedBuffer.reset();
    Assert.assertEquals(joinedBuffer.position(), 2);
    Assert.assertEquals(joinedBuffer.get(), (byte)'C');
  }

  public void testAbsolutePositionSetSynchronizesUnderlyingBuffers () {

    JoinedBuffer joinedBuffer = buffer("AB", "CDE");

    joinedBuffer.position(3);
    Assert.assertEquals(joinedBuffer.get(), (byte)'D');

    joinedBuffer.position(0);
    Assert.assertEquals(joinedBuffer.get(), (byte)'A');
  }

  public void testNegativePositionIsRejected () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    Assert.assertThrows(FormattedIllegalArgumentException.class, () -> joinedBuffer.position(-1));
  }

  public void testPositionBeyondLimitIsRejected () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    Assert.assertThrows(FormattedIllegalArgumentException.class, () -> joinedBuffer.position(5));
  }

  public void testRemainingTracksConsumedBytes () {

    JoinedBuffer joinedBuffer = buffer("AB", "CD");

    joinedBuffer.get();
    Assert.assertEquals(joinedBuffer.remaining(), 3);
    joinedBuffer.get();
    joinedBuffer.get();
    Assert.assertEquals(joinedBuffer.remaining(), 1);
  }
}
