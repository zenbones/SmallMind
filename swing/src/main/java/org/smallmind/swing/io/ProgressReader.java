/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import org.smallmind.swing.progress.ProgressDataHandler;
import org.smallmind.swing.progress.ProgressPanel;

public class ProgressReader extends Reader implements ProgressDataHandler {

  private ProgressPanel progressPanel;
  private Reader reader;
  private char[] separatorArray;
  private long length;
  private long index;
  private long markIndex;

  public ProgressReader (Reader reader, long length, long pulseTime, String lineSeperator) {

    this.reader = reader;
    this.length = length;

    separatorArray = lineSeperator.toCharArray();
    index = 0;
    markIndex = 0;

    progressPanel = new ProgressPanel(this, pulseTime);
  }

  public long getLength () {

    return length;
  }

  public synchronized long getIndex () {

    return index;
  }

  public ProgressPanel getIOProgressPanel () {

    return progressPanel;
  }

  public synchronized int read ()
    throws IOException {

    int readValue;

    if (index >= length) {
      throw new EOFException("Unexpected file termination");
    }

    if ((readValue = reader.read()) >= 0) {
      index++;
    }

    return readValue;
  }

  public synchronized int read (char cbuf[])
    throws IOException {

    int readValue;

    if (index >= length) {
      throw new EOFException("Unexpected file termination");
    }

    if ((readValue = reader.read(cbuf)) >= 0) {
      index += readValue;
    }

    return readValue;
  }

  public synchronized int read (char cbuf[], int off, int len)
    throws IOException {

    int readValue;

    if (index >= length) {
      throw new EOFException("Unexpected file termination");
    }

    if ((readValue = reader.read(cbuf, off, len)) >= 0) {
      index += readValue;
    }

    return readValue;
  }

  public String readLine ()
    throws IOException {

    StringBuilder lineBuilder;
    boolean eol = false;
    int[] bufferArray = new int[separatorArray.length];
    int oneChar;

    if (index >= length) {
      return null;
    }

    for (int count = 0; count < bufferArray.length; count++) {
      bufferArray[count] = 0;
    }

    lineBuilder = new StringBuilder();
    do {
      oneChar = read();
      if (oneChar > 0) {
        if (bufferArray[0] > 0) {
          lineBuilder.append((char)bufferArray[0]);
        }

        System.arraycopy(bufferArray, 1, bufferArray, 0, bufferArray.length - 1);
        bufferArray[bufferArray.length - 1] = oneChar;

        eol = true;
        for (int count = 0; count < bufferArray.length; count++) {
          if (bufferArray[count] != separatorArray[count]) {
            eol = false;
            break;
          }
        }

        if (eol) {
          break;
        }
      }
    } while ((index < length) && (oneChar >= 0));

    if (!eol) {
      for (int bufferToken : bufferArray) {
        if (bufferToken > 0) {
          lineBuilder.append((char)bufferToken);
        }
      }
    }

    if (lineBuilder.length() == 0) {
      return null;
    }

    return lineBuilder.toString();
  }

  public synchronized long skip (long n)
    throws IOException {

    long skipValue;

    if (index >= length) {
      throw new EOFException("Unexpected file termination");
    }

    skipValue = reader.skip(n);
    index += skipValue;

    return skipValue;
  }

  public synchronized void mark (int readAheadLimit)
    throws IOException {

    reader.mark(readAheadLimit);
    markIndex = index;
  }

  public synchronized void reset ()
    throws IOException {

    reader.reset();
    index = markIndex;
  }

  public void close ()
    throws IOException {

    reader.close();
  }

}
