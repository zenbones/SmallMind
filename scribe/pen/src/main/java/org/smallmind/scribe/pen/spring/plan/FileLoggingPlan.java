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
package org.smallmind.scribe.pen.spring.plan;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.Cleanup;
import org.smallmind.scribe.pen.ConsoleAppender;
import org.smallmind.scribe.pen.DateFormatTimestamp;
import org.smallmind.scribe.pen.DefaultErrorHandler;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.FileAppender;
import org.smallmind.scribe.pen.FileSizeQuantifier;
import org.smallmind.scribe.pen.FileSizeRolloverRule;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.LastModifiedCleanupRule;
import org.smallmind.scribe.pen.PatternFormatter;
import org.smallmind.scribe.pen.Rollover;
import org.smallmind.scribe.pen.TimestampQuantifier;
import org.smallmind.scribe.pen.TimestampRolloverRule;

/**
 * Concrete {@link LoggingPlan} that writes log records to a rolling file appender configured with a
 * time-based {@link TimestampRolloverRule}, a size-based {@link FileSizeRolloverRule}, a
 * {@link LastModifiedCleanupRule} for retention, and a {@link ConsoleAppender} as the error handler
 * so that appender failures are reported to the console.
 */
public class FileLoggingPlan extends LoggingPlan {

  private Path logPath = Paths.get("/var/log");
  private DateFormatTimestamp shortTimestamp = new DateFormatTimestamp(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  private DateFormatTimestamp fullTimestamp = new DateFormatTimestamp(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
  private TimestampQuantifier rolloverPeriod = TimestampQuantifier.TOP_OF_DAY;
  private long retentionDays = 31;
  private long rolloverMegabyteLimit = 100;

  /**
   * Sets the root directory path under which log files are written and rolled.
   *
   * @param logPath the directory to write log files to; defaults to {@code /var/log}
   */
  public void setLogPath (Path logPath) {

    this.logPath = logPath;
  }

  /**
   * Sets the timestamp formatter used to generate the date portion of rolled file names; defaults to
   * {@code yyyy-MM-dd} format.
   *
   * @param shortTimestamp the timestamp implementation used when naming rolled-over log files
   */
  public void setShortTimestamp (DateFormatTimestamp shortTimestamp) {

    this.shortTimestamp = shortTimestamp;
  }

  /**
   * Sets the timestamp formatter used when rendering individual log record timestamps inside the log file;
   * defaults to ISO-8601 format with milliseconds and timezone offset.
   *
   * @param fullTimestamp the timestamp implementation used in log record output
   */
  public void setFullTimestamp (DateFormatTimestamp fullTimestamp) {

    this.fullTimestamp = fullTimestamp;
  }

  /**
   * Sets the time boundary at which the log file is rolled over; defaults to {@link TimestampQuantifier#TOP_OF_DAY}.
   *
   * @param rolloverPeriod the time-based quantifier controlling when a new log file is started
   */
  public void setRolloverPeriod (TimestampQuantifier rolloverPeriod) {

    this.rolloverPeriod = rolloverPeriod;
  }

  /**
   * Sets the number of days that rolled log files are retained before the cleanup rule removes them;
   * defaults to 31 days.
   *
   * @param retentionDays the number of days to keep rolled files
   */
  public void setRetentionDays (long retentionDays) {

    this.retentionDays = retentionDays;
  }

  /**
   * Sets the maximum file size in megabytes at which the current log file is rolled over regardless of the
   * time-based period; defaults to 100 MB.
   *
   * @param rolloverMegabyteLimit the size threshold in megabytes that triggers a rollover
   */
  public void setRolloverMegabyteLimit (long rolloverMegabyteLimit) {

    this.rolloverMegabyteLimit = rolloverMegabyteLimit;
  }

  /**
   * Constructs and returns a {@link FileAppender} assembled from the configured rollover rules, cleanup rule,
   * pattern formatter, and a {@link ConsoleAppender}-backed {@link DefaultErrorHandler} that captures
   * appender-level failures.
   *
   * @return a fully configured {@link FileAppender}
   * @throws IOException if the log directory or file cannot be accessed
   */
  @Override
  public Appender getAppender ()
    throws IOException {

    Formatter patternFormatter;
    Cleanup cleanup;
    Rollover rollover;
    Appender consoleAppender;
    ErrorHandler consoleErrorHandler;

    cleanup = new Cleanup('.', new LastModifiedCleanupRule(new Stint(retentionDays, TimeUnit.DAYS)));
    rollover = new Rollover(shortTimestamp, '.', new TimestampRolloverRule(rolloverPeriod), new FileSizeRolloverRule(rolloverMegabyteLimit, FileSizeQuantifier.MEGABYTES));
    patternFormatter = new PatternFormatter(fullTimestamp, "%d %n %+5l (%.1C.%M:%L) [%T] - %m%!+\n\t!p%!+\n\t!s");

    consoleAppender = new ConsoleAppender(patternFormatter);
    consoleErrorHandler = new DefaultErrorHandler(consoleAppender);

    return new FileAppender(logPath, rollover, cleanup, patternFormatter, consoleErrorHandler);
  }
}
