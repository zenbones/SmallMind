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
package org.smallmind.testbench.style;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import com.sun.jdi.connect.TransportTimeoutException;

/**
 * Runs a subprocess and captures its entire standard output into a buffer for later parsing. Used by
 * {@link MavenCommandLocator} to read {@code where} output and by {@link DependencyReducer} to read
 * {@code mvn dependency:analyze} output. The subprocess is expected to exit within three seconds of
 * closing its output stream.
 */
public class ProcessOutputUtility {

  /**
   * Runs {@code commands} as a subprocess in the given working directory, reads its standard output
   * to completion, and returns the captured bytes. After the output stream closes, the process is
   * given three seconds to exit; if it has not, a {@link TransportTimeoutException} (an
   * {@link IOException}) is thrown. Standard error is not captured.
   *
   * @param commandDir the working directory for the subprocess
   * @param commands the command and its arguments
   * @return a {@link ByteArrayOutputStream} holding the full standard output
   * @throws IOException if the process cannot be started, its output cannot be read, or it does not
   * exit within the three-second grace period
   * @throws RuntimeException wrapping an {@link InterruptedException} if the wait for the process to
   * exit is interrupted
   */
  public static ByteArrayOutputStream buffer (Path commandDir, String... commands)
    throws IOException {

    Process process;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try (InputStream processStream = (process = new ProcessBuilder(commands).directory(commandDir.toFile()).start()).getInputStream()) {

      int singleChar;

      while ((singleChar = processStream.read()) >= 0) {
        buffer.write(singleChar);
      }
    }

    try {
      if (!process.waitFor(3, TimeUnit.SECONDS)) {
        throw new TransportTimeoutException();
      }
    } catch (InterruptedException interruptedException) {
      throw new RuntimeException(interruptedException);
    }

    buffer.close();

    return buffer;
  }
}
