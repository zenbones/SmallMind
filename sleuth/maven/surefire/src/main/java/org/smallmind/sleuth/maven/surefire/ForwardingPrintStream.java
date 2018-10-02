/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.sleuth.maven.surefire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;

import static org.apache.maven.surefire.util.internal.StringUtils.NL;

public class ForwardingPrintStream extends PrintStream {

  private final ConsoleOutputReceiver consoleOutputReceiver;
  private final boolean isStdOut;

  ForwardingPrintStream (ConsoleOutputReceiver consoleOutputReceiver, boolean isStdOut) {

    super(new ByteArrayOutputStream());

    this.consoleOutputReceiver = consoleOutputReceiver;
    this.isStdOut = isStdOut;
  }

  @Override
  public void write (byte[] buf, int off, int len) {

    consoleOutputReceiver.writeTestOutput(buf, off, len, isStdOut);
  }

  @Override
  public void write (byte[] b)
    throws IOException {

    consoleOutputReceiver.writeTestOutput(b, 0, b.length, isStdOut);
  }

  @Override
  public void write (int b) {

    try {
      write(new byte[] {(byte)b});
    } catch (IOException ioException) {
      setError();
    }
  }

  @Override
  public void println (String s) {

    byte[] bytes = (((s == null) ? "null" : s) + NL).getBytes();

    consoleOutputReceiver.writeTestOutput(bytes, 0, bytes.length, isStdOut);
  }

  @Override
  public void close () {

  }

  @Override
  public void flush () {

  }
}
