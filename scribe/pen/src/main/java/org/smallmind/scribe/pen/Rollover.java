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

import java.util.Date;

/**
 * Aggregates rollover rules and timestamp formatting for naming rolled log files.
 */
public class Rollover {

  private RolloverRule[] rules;
  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private char separator = '-';

  /**
   * Creates a rollover configuration with default timestamp and separator.
   */
  public Rollover () {

  }

  /**
   * Creates a rollover configuration with the given rules.
   *
   * @param rules rollover rules to apply
   */
  public Rollover (RolloverRule... rules) {

    this.rules = rules;
  }

  /**
   * Creates a rollover configuration with a timestamp provider and rules.
   *
   * @param timestamp timestamp provider
   * @param rules     rollover rules to apply
   */
  public Rollover (Timestamp timestamp, RolloverRule... rules) {

    this.timestamp = timestamp;
    this.rules = rules;
  }

  /**
   * Creates a rollover configuration with timestamp provider, separator, and rules.
   *
   * @param timestamp timestamp provider
   * @param separator separator used in rolled file names
   * @param rules     rollover rules to apply
   */
  public Rollover (Timestamp timestamp, char separator, RolloverRule... rules) {

    this.timestamp = timestamp;
    this.separator = separator;
    this.rules = rules;
  }

  /**
   * Sets the timestamp formatter used when naming rolled files.
   *
   * @param timestamp timestamp provider
   */
  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * Retrieves the separator used between name segments when rolling files.
   *
   * @return separator character
   */
  public char getSeparator () {

    return separator;
  }

  /**
   * Sets the separator inserted between base name, timestamp, and index.
   *
   * @param separator separator character
   */
  public void setSeparator (char separator) {

    this.separator = separator;
  }

  /**
   * Formats the supplied date into a suffix for rolled file names.
   *
   * @param date date to format
   * @return formatted timestamp suffix
   */
  public String getTimestampSuffix (Date date) {

    return timestamp.getTimestamp(date);
  }

  /**
   * Sets the rollover rules evaluated for each log write.
   *
   * @param rules rules to install
   */
  public void setRules (RolloverRule[] rules) {

    this.rules = rules;
  }

  /**
   * Evaluates whether any configured rule triggers rollover.
   *
   * @param fileSize         current file size
   * @param lastModified     last modification time
   * @param bytesToBeWritten pending bytes
   * @return {@code true} if rollover should occur
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
