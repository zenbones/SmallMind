/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class FileAppender extends AbstractFormattedAppender {

  private OutputStream fileOutputStream;
  private Path logPath;
  private Cleanup cleanup;
  private Rollover rollover;
  private boolean closed = false;
  private long fileSize = 0;
  private long lastModified = 0;

  public FileAppender () {

    super();
  }

  public FileAppender (String logFile)
    throws IOException {

    this(Paths.get(logFile), null, null, null, null);
  }

  public FileAppender (Path logPath)
    throws IOException {

    this(logPath, null, null, null, null);
  }

  public FileAppender (String logFile, Rollover rollover)
    throws IOException {

    this(Paths.get(logFile), rollover, null, null, null);
  }

  public FileAppender (Path logPath, Rollover rollover)
    throws IOException {

    this(logPath, rollover, null, null, null);
  }

  public FileAppender (String logFile, Cleanup cleanup)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, null, null);
  }

  public FileAppender (Path logPath, Cleanup cleanup)
    throws IOException {

    this(logPath, null, cleanup, null, null);
  }

  public FileAppender (String logFile, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), null, null, formatter, null);
  }

  public FileAppender (Path logPath, Formatter formatter)
    throws IOException {

    this(logPath, null, null, formatter, null);
  }

  public FileAppender (String logFile, Rollover rollover, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), rollover, null, formatter, null);
  }

  public FileAppender (Path logPath, Rollover rollover, Formatter formatter)
    throws IOException {

    this(logPath, rollover, null, formatter, null);
  }

  public FileAppender (String logFile, Cleanup cleanup, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, formatter, null);
  }

  public FileAppender (Path logPath, Cleanup cleanup, Formatter formatter)
    throws IOException {

    this(logPath, null, cleanup, formatter, null);
  }

  public FileAppender (String logFile, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), null, null, formatter, errorHandler);
  }

  public FileAppender (Path logPath, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, null, null, formatter, errorHandler);
  }

  public FileAppender (String logFile, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), rollover, null, formatter, errorHandler);
  }

  public FileAppender (Path logPath, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, rollover, null, formatter, errorHandler);
  }

  public FileAppender (String logFile, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, formatter, errorHandler);
  }

  public FileAppender (Path logPath, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, null, cleanup, formatter, errorHandler);
  }

  public FileAppender (String logFile, Rollover rollover, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), rollover, cleanup, formatter, errorHandler);
  }

  public FileAppender (Path logPath, Rollover rollover, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    super(formatter, errorHandler);

    this.rollover = rollover;

    setLogPath(logPath);
  }

  public Rollover getRollover () {

    return rollover;
  }

  public void setRollover (Rollover rollover) {

    this.rollover = rollover;
  }

  public Cleanup getCleanup () {

    return cleanup;
  }

  public void setCleanup (Cleanup cleanup) {

    this.cleanup = cleanup;
  }

  public Path getLogPath () {

    return logPath;
  }

  public void setLogPath (Path logPath)
    throws IOException {

    this.logPath = logPath;

    try {

      Path parentPath;

      if ((parentPath = logPath.getParent()) != null) {
        Files.createDirectories(parentPath);
      }

      Files.createFile(logPath);
    } catch (IOException ioException) {
      throw new IOException("Error trying to instantiate the requested file(" + logPath.toAbsolutePath() + ")");
    }

    if (Files.isDirectory(logPath)) {
      throw new IOException("File must specify a non-directory path(" + logPath.toAbsolutePath() + ")");
    }

    fileOutputStream = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  public void setLogFilePath (String logFilePath)
    throws IOException {

    setLogPath(Paths.get(logFilePath));
  }

  public synchronized void handleOutput (String formattedOutput)
    throws LoggerException {

    byte[] formattedBytes = formattedOutput.getBytes();

    if (closed) {
      throw new LoggerException("Appender to file(%s) has been previously closed", logPath.toAbsolutePath());
    }

    if (fileOutputStream != null) {
      if ((rollover != null) && rollover.willRollover(fileSize, lastModified, formattedBytes.length)) {

        Path rolloverPath;
        Path parentPath = logPath.getParent();
        StringBuilder rolloverNameBuilder;
        StringBuilder uniqueNameBuilder;
        String logFileName;
        int dotPos;
        int uniqueCount = 0;

        rolloverNameBuilder = new StringBuilder();
        logFileName = logPath.getFileName().toString();

        if ((dotPos = logFileName.lastIndexOf('.')) >= 0) {
          rolloverNameBuilder.append(logFileName.substring(0, dotPos));
        } else {
          rolloverNameBuilder.append(logFileName);
        }

        rolloverNameBuilder.append(rollover.getSeparator());
        rolloverNameBuilder.append(rollover.getTimestampSuffix(new Date()));

        do {
          uniqueNameBuilder = new StringBuilder(rolloverNameBuilder);
          uniqueNameBuilder.append(rollover.getSeparator());
          uniqueNameBuilder.append(uniqueCount++);

          if (dotPos >= 0) {
            uniqueNameBuilder.append(logFileName.substring(dotPos));
          }
        } while (Files.exists(rolloverPath = (parentPath == null) ? Paths.get(uniqueNameBuilder.toString()) : parentPath.resolve(uniqueNameBuilder.toString())));

        try {
          fileOutputStream.close();
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Unable to close the current log file(%s)", logPath.toAbsolutePath());
        }
        try {
          Files.move(logPath, rolloverPath);
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Could not rollover the log file to the archive name(%s)", rolloverPath.toAbsolutePath());
        }
        try {
          fileOutputStream = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Unable to create the new log file(%s)", logPath.toAbsolutePath());
        }

        try {
          cleanup.vacuum(logPath);
        } catch (IOException ioException) {
          throw new LoggerException(ioException);
        }
      }

      try {
        fileOutputStream.write(formattedBytes);
        fileOutputStream.flush();

        fileSize += formattedBytes.length;
        lastModified = System.currentTimeMillis();
      } catch (IOException ioException) {
        throw new LoggerException(ioException, "Error attempting to output to file(%s)", logPath.toAbsolutePath());
      }
    }
  }

  public synchronized void close ()
    throws LoggerException {

    if (!closed) {
      closed = true;

      try {
        fileOutputStream.close();
      } catch (IOException ioException) {
        throw new LoggerException(ioException);
      }
    }
  }

  public void finalize ()
    throws LoggerException {

    close();
  }
}