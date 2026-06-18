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
 * Utility for launching a subprocess and buffering its entire standard output for subsequent
 * parsing. The process must complete within 3 seconds after its output stream is fully consumed;
 * callers receive a {@link ByteArrayOutputStream} containing the captured bytes.
 */
public class ProcessOutputUtility {

  /**
   * Execute {@code commands} as a subprocess, capture its standard output, and return the buffer.
   *
   * <p>The process is started with {@code commandDir} as its working directory. Standard output is
   * read to completion before waiting for the process to exit. If the process has not exited
   * within 3 seconds after the output stream closes, a {@link TransportTimeoutException} is thrown.
   *
   * @param commandDir working directory for the subprocess
   * @param commands   the command and its arguments
   * @return a {@link ByteArrayOutputStream} containing the full captured standard output
   * @throws IOException      if the process cannot be started or its output stream cannot be read
   * @throws RuntimeException wrapping {@link InterruptedException} if the wait for process exit is interrupted
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
