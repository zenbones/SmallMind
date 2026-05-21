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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PacketWriterTest {

  private StringBuilder builder;
  private PacketWriter writer;

  @BeforeMethod
  public void beforeMethod () {

    builder = new StringBuilder();
    writer = new PacketWriter(builder);
  }

  public void testWriteIntAppendsAsChar () {

    writer.write('A');
    writer.write('B');

    Assert.assertEquals(builder.toString(), "AB");
  }

  public void testWriteIntTruncatesToCharRange () {

    writer.write(0x10041);

    Assert.assertEquals(builder.toString(), "A");
  }

  public void testWriteCharArrayFullRange () {

    char[] data = {'h', 'e', 'l', 'l', 'o'};
    writer.write(data, 0, data.length);

    Assert.assertEquals(builder.toString(), "hello");
  }

  public void testWriteCharArrayPartialRange () {

    char[] data = {'h', 'e', 'l', 'l', 'o'};
    writer.write(data, 1, 3);

    Assert.assertEquals(builder.toString(), "ell");
  }

  public void testWriteCharArrayZeroLengthIsNoOp () {

    char[] data = {'a', 'b', 'c'};
    writer.write(data, 0, 0);

    Assert.assertEquals(builder.length(), 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testWriteCharArrayNullThrows () {

    writer.write((char[])null, 0, 0);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testWriteCharArrayNegativeOffsetThrows () {

    writer.write(new char[] {'a'}, -1, 1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testWriteCharArrayNegativeLengthThrows () {

    writer.write(new char[] {'a'}, 0, -1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testWriteCharArrayBeyondEndThrows () {

    writer.write(new char[] {'a', 'b'}, 0, 3);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testWriteCharArrayIntegerOverflowThrows () {

    writer.write(new char[] {'a'}, Integer.MAX_VALUE, 1);
  }

  public void testWriteStringAppends () {

    writer.write("hello");

    Assert.assertEquals(builder.toString(), "hello");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testWriteStringNullThrows () {

    writer.write((String)null);
  }

  public void testWriteStringSubstringAppends () {

    writer.write("hello world", 6, 5);

    Assert.assertEquals(builder.toString(), "world");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testWriteStringSubstringNullThrows () {

    writer.write((String)null, 0, 0);
  }

  public void testMultipleWritesAccumulate () {

    writer.write('[');
    writer.write("foo");
    writer.write(new char[] {',', 'b', 'a', 'r'}, 0, 4);
    writer.write(']');

    Assert.assertEquals(builder.toString(), "[foo,bar]");
  }

  public void testFlushIsNoOp () {

    writer.write("data");
    writer.flush();

    Assert.assertEquals(builder.toString(), "data");
  }

  public void testCloseIsNoOpAndAllowsFurtherWrites () {

    writer.write("before");
    writer.close();
    writer.write("after");

    Assert.assertEquals(builder.toString(), "beforeafter");
  }

  public void testToStringReflectsBuilderContents () {

    writer.write("snapshot");

    Assert.assertEquals(writer.toString(), builder.toString());
  }

  public void testExternalBuilderMutationIsVisible () {

    builder.append("seed:");
    writer.write("rest");

    Assert.assertEquals(builder.toString(), "seed:rest");
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testWriteCharArrayOffsetBeyondLengthThrows () {

    writer.write(new char[] {'a', 'b'}, 5, 1);
  }
}
