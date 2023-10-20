/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.Writer;

public class PacketWriter extends Writer {

  private final StringBuilder builder;

  public PacketWriter (StringBuilder builder) {

    this.builder = builder;
  }

  public void write (int c) {

    builder.append((char)c);
  }

  public void write (char cbuf[], int off, int len) {

    if (cbuf == null) {
      throw new NullPointerException();
    } else {
      if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
      } else if (len > 0) {
        builder.append(cbuf, off, len);
      }
    }
  }

  public void write (String str) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str);
    }
  }

  public void write (String str, int off, int len) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str, off, off + len);
    }
  }

  @Override
  public void flush () {

  }

  @Override
  public void close () {

  }

  public String toString () {

    return builder.toString();
  }
}
