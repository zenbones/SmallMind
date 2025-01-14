/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.reverse.http1_1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.smallmind.nutsnbolts.io.ByteArrayIOStream;

public class HttpProtocolInputStream extends InputStream {

  private final ByteArrayIOStream.ByteArrayInputStream byteArrayInputStream;
  private final int index = 0;
  BufferedInputStream b;

  public HttpProtocolInputStream (ByteArrayIOStream.ByteArrayInputStream byteArrayInputStream) {

    this.byteArrayInputStream = byteArrayInputStream;
  }

  @Override
  public synchronized void mark (int readlimit) {

    byteArrayInputStream.mark(readlimit);
  }

  @Override
  public synchronized void reset ()
    throws IOException {

    byteArrayInputStream.reset();
  }

  @Override
  public int read ()
    throws IOException {

    return (byteArrayInputStream.available() == 0) ? -1 : byteArrayInputStream.read();
  }

  public String readLine ()
    throws IOException {

    if (byteArrayInputStream.available() > 0) {

      StringBuilder lineBuilder = new StringBuilder();

      do {

        int currentChar;

        if (((currentChar = byteArrayInputStream.read()) == '\n') && (lineBuilder.charAt(lineBuilder.length() - 1) == '\r')) {

          return lineBuilder.substring(0, lineBuilder.length() - 1);
        } else {
          lineBuilder.append((char)currentChar);
        }
      } while (byteArrayInputStream.available() > 0);
    }

    return null;
  }
}
