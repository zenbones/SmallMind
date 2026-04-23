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

import java.time.LocalDateTime;

/**
 * Aggregates one or more {@link RolloverRule} instances and a {@link Timestamp} provider to determine
 * when a log file should be rotated and how the rolled file should be named.
 */
public class Rollover {

  private RolloverRule[] rules;
  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private char separator = '-';

  /**
   * Constructs a rollover configuration with the default {@link DateFormatTimestamp} and a {@code '-'} separator.
   */
  public Rollover () {

  }

  /**
   * Constructs a rollover configuration with the default timestamp, default separator, and the given rules.
   *
   * @param rules the rollover rules to evaluate on each log write
   */
  public Rollover (RolloverRule... rules) {

    this.rules = rules;
  }

  /**
   * Constructs a rollover configuration with an explicit timestamp provider and rules.
   *
   * @param timestamp the {@link Timestamp} used to format the suffix of rolled file names
   * @param rules     the rollover rules to evaluate on each log write
   */
  public Rollover (Timestamp timestamp, RolloverRule... rules) {

    this.timestamp = timestamp;
    this.rules = rules;
  }

  /**
   * Constructs a fully specified rollover configuration with a timestamp provider, name separator, and rules.
   *
   * @param timestamp the {@link Timestamp} used to format the suffix of rolled file names
   * @param separator the character inserted between the base file name, timestamp, and index segments
   * @param rules     the rollover rules to evaluate on each log write
   */
  public Rollover (Timestamp timestamp, char separator, RolloverRule... rules) {

    this.timestamp = timestamp;
    this.separator = separator;
    this.rules = rules;
  }

  /**
   * Replaces the timestamp provider used when generating rolled file name suffixes.
   *
   * @param timestamp the new {@link Timestamp} implementation to use
   */
  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * Returns the separator character placed between the base file name, timestamp, and optional index.
   *
   * @return the current separator character
   */
  public char getSeparator () {

    return separator;
  }

  /**
   * Sets the separator character inserted between the base file name, timestamp, and optional index
   * when constructing rolled file names.
   *
   * @param separator the new separator character
   */
  public void setSeparator (char separator) {

    this.separator = separator;
  }

  /**
   * Formats the given date into a timestamp suffix for use in a rolled file name.
   *
   * @param date the date to format
   * @return the formatted timestamp string produced by the configured {@link Timestamp}
   */
  public String getTimestampSuffix (LocalDateTime date) {

    return timestamp.getTimestamp(date);
  }

  /**
   * Replaces the set of rollover rules evaluated on each log write.
   *
   * @param rules the new rollover rules to use
   */
  public void setRules (RolloverRule[] rules) {

    this.rules = rules;
  }

  /**
   * Returns {@code true} if any configured rule determines that the file should be rolled over,
   * short-circuiting evaluation after the first triggered rule.
   *
   * @param fileSize         the current size of the log file in bytes
   * @param lastModified     the last modification time of the log file in milliseconds since epoch
   * @param bytesToBeWritten the number of bytes about to be written to the file
   * @return {@code true} if at least one rule triggers rollover; {@code false} otherwise
   */
  public boolean willRollover (long fileSize, long lastModified, long bytesToBeWritten) {

    for (RolloverRule rule : rules) {
      if (rule.willRollover(fileSize, lastModified, bytesToBeWritten)) {

        return true;
      }
    }

    return false;
  }
}
