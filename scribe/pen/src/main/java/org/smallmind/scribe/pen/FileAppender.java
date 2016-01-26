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
package org.smallmind.scribe.pen;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class FileAppender extends AbstractFormattedAppender {

  private BufferedOutputStream fileOutputStream;
  private File logFile;
  private Rollover rollover;
  private boolean closed = false;

  public FileAppender () {

    super();
  }

  public FileAppender (String logFilePath)
    throws IOException {

    this(logFilePath, null, null, null);
  }

  public FileAppender (File logFile)
    throws IOException {

    this(logFile, null, null, null);
  }

  public FileAppender (String logFilePath, Rollover rollover)
    throws IOException {

    this(logFilePath, rollover, null, null);
  }

  public FileAppender (File logFile, Rollover rollover)
    throws IOException {

    this(logFile, rollover, null, null);
  }

  public FileAppender (String logFilePath, Formatter formatter)
    throws IOException {

    this(logFilePath, null, formatter, null);
  }

  public FileAppender (File logFile, Formatter formatter)
    throws IOException {

    this(logFile, null, formatter, null);
  }

  public FileAppender (String logFilePath, Rollover rollover, Formatter formatter)
    throws IOException {

    this(logFilePath, rollover, formatter, null);
  }

  public FileAppender (File logFile, Rollover rollover, Formatter formatter)
    throws IOException {

    this(logFile, rollover, formatter, null);
  }

  public FileAppender (String logFilePath, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logFilePath, null, formatter, errorHandler);
  }

  public FileAppender (File logFile, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logFile, null, formatter, errorHandler);
  }

  public FileAppender (String logFilePath, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(new File(logFilePath), rollover, formatter, errorHandler);
  }

  public FileAppender (File logFile, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    super(formatter, errorHandler);

    this.rollover = rollover;

    setLogFile(logFile);
  }

  public Rollover getRollover () {

    return rollover;
  }

  public void setRollover (Rollover rollover) {

    this.rollover = rollover;
  }

  public File getLogFile () {

    return logFile;
  }

  public void setLogFilePath (String logFilePath)
    throws IOException {

    setLogFile(new File(logFilePath));
  }

  public void setLogFile (File logFile)
    throws IOException {

    this.logFile = logFile;

    try {

      File parentDirectory;

      if ((parentDirectory = logFile.getParentFile()) != null) {
        parentDirectory.mkdirs();
      }

      logFile.createNewFile();
    }
    catch (IOException ioException) {
      throw new IOException("Error trying to instantiate the requested file(" + logFile.getCanonicalPath() + ")");
    }

    if (!logFile.isFile()) {
      throw new IOException("File must specify a non-directory path(" + logFile.getCanonicalPath() + ")");
    }

    fileOutputStream = new BufferedOutputStream(new FileOutputStream(logFile, true));
  }

  public synchronized void handleOutput (String formattedOutput)
    throws LoggerException {

    byte[] formattedBytes = formattedOutput.getBytes();

    if (closed) {
      throw new LoggerException("Appender to file(%s) has been previously closed", logFile.getAbsolutePath());
    }

    if (logFile != null) {
      try {
        if ((rollover != null) && rollover.willRollover(logFile, formattedBytes.length)) {

          File rolloverFile;
          StringBuilder rolloverPathBuilder;
          StringBuilder uniquePathBuilder;
          String parentPath;
          String logFileName;
          int dotPos;
          int uniqueCount = 0;

          rolloverPathBuilder = new StringBuilder();

          if ((parentPath = logFile.getParent()) != null) {
            rolloverPathBuilder.append(parentPath);
            rolloverPathBuilder.append(System.getProperty("file.separator"));
          }

          logFileName = logFile.getName();

          if ((dotPos = logFileName.lastIndexOf('.')) >= 0) {
            rolloverPathBuilder.append(logFileName.substring(0, dotPos));
          }
          else {
            rolloverPathBuilder.append(logFileName);
          }

          rolloverPathBuilder.append(rollover.getSeparator());
          rolloverPathBuilder.append(rollover.getTimestampSuffix(new Date()));

          do {
            uniquePathBuilder = new StringBuilder(rolloverPathBuilder.toString());
            uniquePathBuilder.append(rollover.getSeparator());
            uniquePathBuilder.append(uniqueCount++);

            if (dotPos >= 0) {
              uniquePathBuilder.append(logFileName.substring(dotPos));
            }

            rolloverFile = new File(uniquePathBuilder.toString());
          } while (rolloverFile.exists());

          fileOutputStream.close();

          if (!logFile.renameTo(rolloverFile)) {
            throw new LoggerException("Could not rollover the log file to the archive name(%s)", rolloverFile.getAbsolutePath());
          }

          if (!logFile.createNewFile()) {
            throw new LoggerException("Could not recreate the log file(%s) after rollover", logFile.getAbsolutePath());
          }
          fileOutputStream = new BufferedOutputStream(new FileOutputStream(logFile, true));
        }

        fileOutputStream.write(formattedBytes);
        fileOutputStream.flush();
      }
      catch (IOException ioException) {
        throw new LoggerException(ioException, "Error attempting to output to file(%s)", logFile.getAbsolutePath());
      }
    }
  }

  public synchronized void close ()
    throws LoggerException {

    if (!closed) {
      closed = true;

      try {
        fileOutputStream.close();
      }
      catch (IOException ioException) {
        throw new LoggerException(ioException);
      }
    }
  }

  public void finalize ()
    throws LoggerException {

    close();
  }
}