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
package org.smallmind.nutsnbolts.lang;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Pairs an {@link InputStream} of class bytes with the last-modified timestamp of the source, and implements {@link Closeable} to ensure the stream is properly released.
 */
public class ClassStreamTicket implements Closeable {

  private final InputStream inputStream;
  private final long timeStamp;

  /**
   * Creates a ticket wrapping the given class byte stream and its associated modification timestamp.
   *
   * @param inputStream the stream supplying the raw class bytes
   * @param timeStamp   the last-modified time of the class source, in milliseconds since the epoch
   */
  public ClassStreamTicket (InputStream inputStream, long timeStamp) {

    this.inputStream = inputStream;
    this.timeStamp = timeStamp;
  }

  /**
   * Returns the stream of raw class bytes.
   *
   * @return the open {@link InputStream} for the class data
   */
  public InputStream getInputStream () {

    return inputStream;
  }

  /**
   * Returns the last-modified timestamp associated with the class source.
   *
   * @return the modification time in milliseconds since the epoch
   */
  public long getTimeStamp () {

    return timeStamp;
  }

  /**
   * Closes the underlying class byte stream.
   *
   * @throws IOException if an I/O error occurs while closing the stream
   */
  @Override
  public void close ()
    throws IOException {

    inputStream.close();
  }
}
