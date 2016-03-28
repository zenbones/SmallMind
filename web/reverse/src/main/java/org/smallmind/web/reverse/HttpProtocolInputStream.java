/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.reverse;

import java.io.InputStream;

public class HttpProtocolInputStream extends InputStream {

  private final byte[] buffer;
  private int index = 0;

  public HttpProtocolInputStream (byte[] buffer) {

    this.buffer = buffer;
  }

  @Override
  public int read () {

    return (index == buffer.length) ? -1 : buffer[index++];
  }

  public String readLine () {

    if (index == buffer.length) {

      return null;
    }

    int lastChar = 0;
    int lineIndex = index;

    while (!((buffer[lineIndex] == '\n') && (lastChar == '\r'))) {
      if (lineIndex == buffer.length) {

        return null;
      }
      lastChar = buffer[lineIndex++];
    }

    try {
      return new String(buffer, index, lineIndex - 1);
    } finally {
      index = lineIndex + 1;
    }
  }
}
