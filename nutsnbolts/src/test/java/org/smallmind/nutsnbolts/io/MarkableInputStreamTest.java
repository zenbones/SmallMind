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
public class MarkableInputStreamTest {

  public void testMarkSupportedReturnsTrue () {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[0]));

    Assert.assertTrue(stream.markSupported());
  }

  public void testMarkAndResetReplaysBufferedBytes ()
    throws IOException {

    byte[] payload = "abcdef".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    stream.mark(4);
    byte[] firstRead = stream.readNBytes(3);
    stream.reset();
    byte[] secondRead = stream.readNBytes(3);

    Assert.assertEquals(firstRead, secondRead);
  }

  @Test(expectedExceptions = IOException.class)
  public void testResetWithoutMarkThrows ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));

    stream.reset();
  }

  @Test(expectedExceptions = IOException.class)
  public void testReadAfterCloseThrows ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    stream.close();
    stream.read();
  }

  public void testCloseIsIdempotent ()
    throws IOException {

    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    stream.close();
    stream.close();
  }

  public void testReadAllBytesReturnsCompleteContent ()
    throws IOException {

    byte[] payload = "complete-payload".getBytes(StandardCharsets.UTF_8);
    MarkableInputStream stream = new MarkableInputStream(new ByteArrayInputStream(payload));

    Assert.assertEquals(stream.readAllBytes(), payload);
  }
}
