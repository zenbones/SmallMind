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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Appender that writes formatted log output to a file, with optional {@link Rollover} and {@link Cleanup}
 * policies for managing log rotation and pruning of old archived files.
 */
public class FileAppender extends AbstractFormattedAppender {

  private OutputStream outputStream;
  private Path logPath;
  private Cleanup cleanup;
  private Rollover rollover;
  private boolean closed = false;
  private long fileSize = 0;
  private long lastModified = 0;

  /**
   * Constructs a file appender with no log path, formatter, rollover policy, or error handler; the log path
   * must be supplied via {@link #setLogPath(Path)} or {@link #setLogFile(String)} before use.
   */
  public FileAppender () {

    super();
  }

  /**
   * Constructs a file appender that writes to the file at the given path string, creating parent
   * directories if they do not exist.
   *
   * @param logFile path string identifying the log file
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile)
    throws IOException {

    this(Paths.get(logFile), null, null, null, null);
  }

  /**
   * Constructs a file appender that writes to the given path, creating parent directories if they do not exist.
   *
   * @param logPath path identifying the log file
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath)
    throws IOException {

    this(logPath, null, null, null, null);
  }

  /**
   * Constructs a file appender with a rollover policy that rotates the log file when triggered.
   *
   * @param logFile  path string identifying the log file
   * @param rollover policy that determines when and how the log file is rotated
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Rollover rollover)
    throws IOException {

    this(Paths.get(logFile), rollover, null, null, null);
  }

  /**
   * Constructs a file appender with a rollover policy that rotates the log file when triggered.
   *
   * @param logPath  path identifying the log file
   * @param rollover policy that determines when and how the log file is rotated
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Rollover rollover)
    throws IOException {

    this(logPath, rollover, null, null, null);
  }

  /**
   * Constructs a file appender with a cleanup policy that prunes archived log files after rollover.
   *
   * @param logFile path string identifying the log file
   * @param cleanup policy that removes old archived log files after each rollover
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Cleanup cleanup)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, null, null);
  }

  /**
   * Constructs a file appender with a cleanup policy that prunes archived log files after rollover.
   *
   * @param logPath path identifying the log file
   * @param cleanup policy that removes old archived log files after each rollover
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Cleanup cleanup)
    throws IOException {

    this(logPath, null, cleanup, null, null);
  }

  /**
   * Constructs a file appender that applies the given formatter before writing to the log file.
   *
   * @param logFile   path string identifying the log file
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), null, null, formatter, null);
  }

  /**
   * Constructs a file appender that applies the given formatter before writing to the log file.
   *
   * @param logPath   path identifying the log file
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Formatter formatter)
    throws IOException {

    this(logPath, null, null, formatter, null);
  }

  /**
   * Constructs a file appender with a rollover policy and a formatter.
   *
   * @param logFile   path string identifying the log file
   * @param rollover  policy that determines when and how the log file is rotated
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Rollover rollover, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), rollover, null, formatter, null);
  }

  /**
   * Constructs a file appender with a rollover policy and a formatter.
   *
   * @param logPath   path identifying the log file
   * @param rollover  policy that determines when and how the log file is rotated
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Rollover rollover, Formatter formatter)
    throws IOException {

    this(logPath, rollover, null, formatter, null);
  }

  /**
   * Constructs a file appender with a cleanup policy and a formatter.
   *
   * @param logFile   path string identifying the log file
   * @param cleanup   policy that removes old archived log files after each rollover
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Cleanup cleanup, Formatter formatter)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, formatter, null);
  }

  /**
   * Constructs a file appender with a cleanup policy and a formatter.
   *
   * @param logPath   path identifying the log file
   * @param cleanup   policy that removes old archived log files after each rollover
   * @param formatter the formatter used to convert log records to strings
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Cleanup cleanup, Formatter formatter)
    throws IOException {

    this(logPath, null, cleanup, formatter, null);
  }

  /**
   * Constructs a file appender with a formatter and an error handler.
   *
   * @param logFile      path string identifying the log file
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), null, null, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with a formatter and an error handler.
   *
   * @param logPath      path identifying the log file
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, null, null, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with a rollover policy, a formatter, and an error handler.
   *
   * @param logFile      path string identifying the log file
   * @param rollover     policy that determines when and how the log file is rotated
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), rollover, null, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with a rollover policy, a formatter, and an error handler.
   *
   * @param logPath      path identifying the log file
   * @param rollover     policy that determines when and how the log file is rotated
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Rollover rollover, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, rollover, null, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with a cleanup policy, a formatter, and an error handler.
   *
   * @param logFile      path string identifying the log file
   * @param cleanup      policy that removes old archived log files after each rollover
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), null, cleanup, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with a cleanup policy, a formatter, and an error handler.
   *
   * @param logPath      path identifying the log file
   * @param cleanup      policy that removes old archived log files after each rollover
   * @param formatter    the formatter used to convert log records to strings
   * @param errorHandler the handler invoked when output or formatting fails
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(logPath, null, cleanup, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with the full complement of rollover policy, cleanup policy,
   * formatter, and error handler.
   *
   * @param logFile      path string identifying the log file
   * @param rollover     policy that determines when and how the log file is rotated, or {@code null} for none
   * @param cleanup      policy that removes old archived log files after each rollover, or {@code null} for none
   * @param formatter    the formatter used to convert log records to strings, or {@code null} for no formatting
   * @param errorHandler the handler invoked when output or formatting fails, or {@code null} to discard errors
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public FileAppender (String logFile, Rollover rollover, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    this(Paths.get(logFile), rollover, cleanup, formatter, errorHandler);
  }

  /**
   * Constructs a file appender with the full complement of rollover policy, cleanup policy,
   * formatter, and error handler.
   *
   * @param logPath      path identifying the log file
   * @param rollover     policy that determines when and how the log file is rotated, or {@code null} for none
   * @param cleanup      policy that removes old archived log files after each rollover, or {@code null} for none
   * @param formatter    the formatter used to convert log records to strings, or {@code null} for no formatting
   * @param errorHandler the handler invoked when output or formatting fails, or {@code null} to discard errors
   * @throws IOException if the path is a directory or the output stream cannot be opened
   */
  public FileAppender (Path logPath, Rollover rollover, Cleanup cleanup, Formatter formatter, ErrorHandler errorHandler)
    throws IOException {

    super(formatter, errorHandler);

    this.rollover = rollover;
    this.cleanup = cleanup;

    setLogPath(logPath);
  }

  /**
   * Returns the rollover policy currently assigned to this appender.
   *
   * @return the active rollover policy, or {@code null} if no rollover policy is configured
   */
  public Rollover getRollover () {

    return rollover;
  }

  /**
   * Sets the rollover policy used to rotate the log file when size or age thresholds are met.
   *
   * @param rollover the rollover policy to apply, or {@code null} to disable rollover
   */
  public void setRollover (Rollover rollover) {

    this.rollover = rollover;
  }

  /**
   * Returns the cleanup policy currently assigned to this appender.
   *
   * @return the active cleanup policy, or {@code null} if no cleanup policy is configured
   */
  public Cleanup getCleanup () {

    return cleanup;
  }

  /**
   * Sets the cleanup policy used to prune old archived log files after each rollover.
   *
   * @param cleanup the cleanup policy to apply, or {@code null} to disable cleanup
   */
  public void setCleanup (Cleanup cleanup) {

    this.cleanup = cleanup;
  }

  /**
   * Returns the path of the current active log file.
   *
   * @return the active log file path
   */
  public Path getLogPath () {

    return logPath;
  }

  /**
   * Sets the target log file path, creating any missing parent directories and opening a new
   * append-mode output stream to the file.
   *
   * @param logPath path to the log file; must not refer to an existing directory
   * @throws IOException if the path is a directory, parent directories cannot be created,
   *                     or the output stream cannot be opened
   */
  public void setLogPath (Path logPath)
    throws IOException {

    this.logPath = logPath;

    if (Files.isDirectory(logPath)) {
      throw new IOException("File must specify a non-directory path(" + logPath.toAbsolutePath() + ")");
    } else {

      Path parentPath;

      if ((parentPath = logPath.getParent()) != null) {
        Files.createDirectories(parentPath);
      }

      openStream();
    }
  }

  /**
   * Sets the target log file path from a string, creating parent directories and opening the output stream.
   *
   * @param logFile path string identifying the log file; must not refer to an existing directory
   * @throws IOException if the path cannot be resolved, is a directory, or the output stream cannot be opened
   */
  public void setLogFile (String logFile)
    throws IOException {

    setLogPath(Paths.get(logFile));
  }

  /**
   * Opens an append-mode output stream to the current log path and resets the internal
   * file-size counter and last-modified timestamp.
   *
   * @throws IOException if the output stream cannot be created or the file attributes cannot be read
   */
  private void openStream ()
    throws IOException {

    outputStream = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    lastModified = Files.getLastModifiedTime(logPath).toMillis();
    fileSize = 0;
  }

  /**
   * Writes formatted UTF-8 text to the log file. If the rollover policy determines that the
   * file should be rotated before writing, the current file is renamed to an archive path, a new
   * file is opened, and the cleanup policy (if present) is invoked. This method is synchronized
   * so that concurrent callers do not interleave their output or trigger simultaneous rollovers.
   *
   * @param formattedOutput the fully formatted text to append to the log file; must not be {@code null}
   * @throws LoggerException if this appender has already been closed, if the rollover or file-move
   *                         operations fail, if a new output stream cannot be opened, if the cleanup
   *                         policy raises an {@link java.io.IOException}, or if the write itself fails
   */
  public synchronized void handleOutput (String formattedOutput)
    throws LoggerException {

    byte[] formattedBytes = formattedOutput.getBytes(StandardCharsets.UTF_8);

    if (closed) {
      throw new LoggerException("Appender to file(%s) has been previously closed", logPath.toAbsolutePath());
    }

    if (outputStream != null) {
      if ((rollover != null) && rollover.willRollover(fileSize, lastModified, formattedBytes.length)) {

        Path rolloverPath = FileNameUtility.calculateUniquePath(logPath, rollover.getSeparator(), rollover.getTimestampSuffix(LocalDateTime.now()), true);

        try {
          outputStream.close();
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Unable to close the current log file(%s)", logPath.toAbsolutePath());
        }
        try {
          Files.move(logPath, rolloverPath);
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Could not rollover the log file to the archive name(%s)", rolloverPath.toAbsolutePath());
        }
        try {
          openStream();
        } catch (IOException ioException) {
          throw new LoggerException(ioException, "Unable to create the new log file(%s)", logPath.toAbsolutePath());
        }

        if (cleanup != null) {
          try {
            cleanup.vacuum(logPath);
          } catch (IOException ioException) {
            throw new LoggerException(ioException);
          }
        }
      }

      try {
        outputStream.write(formattedBytes);
        outputStream.flush();

        fileSize += formattedBytes.length;
        lastModified = System.currentTimeMillis();
      } catch (IOException ioException) {
        throw new LoggerException(ioException, "Error attempting to output to file(%s)", logPath.toAbsolutePath());
      }
    }
  }

  /**
   * Closes the underlying output stream and marks this appender as closed, preventing any further
   * writes. Subsequent calls to {@link #handleOutput} will throw a {@link LoggerException}. If the
   * appender is already closed, this method does nothing.
   *
   * @throws LoggerException if the output stream cannot be closed cleanly
   */
  public synchronized void close ()
    throws LoggerException {

    if (!closed) {
      closed = true;

      try {
        outputStream.close();
      } catch (IOException ioException) {
        throw new LoggerException(ioException);
      }
    }
  }
}
