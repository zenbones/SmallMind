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

/**
 * Rollover rule that triggers when the log file exceeds a maximum size.
 */
public class FileSizeRolloverRule implements RolloverRule {

  private FileSizeQuantifier fileSizeQuantifier;
  private long maxSize;

  /**
   * Creates a rollover rule with the default threshold of 10 megabytes.
   */
  public FileSizeRolloverRule () {

    this(10, FileSizeQuantifier.MEGABYTES);
  }

  /**
   * Creates a rollover rule with the provided size and quantifier.
   *
   * @param maxSize            size threshold before rollover
   * @param fileSizeQuantifier unit multiplier for the size
   */
  public FileSizeRolloverRule (long maxSize, FileSizeQuantifier fileSizeQuantifier) {

    super();

    this.maxSize = maxSize;
    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  /**
   * Retrieves the configured maximum file size threshold.
   *
   * @return maximum size before rollover
   */
  public long getMaxSize () {

    return maxSize;
  }

  /**
   * Sets the maximum file size before rollover.
   *
   * @param maxSize size threshold in units of {@link FileSizeQuantifier}
   */
  public void setMaxSize (long maxSize) {

    this.maxSize = maxSize;
  }

  /**
   * Returns the quantifier used to interpret the maximum size.
   *
   * @return size quantifier
   */
  public FileSizeQuantifier getFileSizeQuantifier () {

    return fileSizeQuantifier;
  }

  /**
   * Sets the size quantifier multiplier.
   *
   * @param fileSizeQuantifier quantifier to use
   */
  public void setFileSizeQuantifier (FileSizeQuantifier fileSizeQuantifier) {

    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  /**
   * Determines whether writing the pending bytes would exceed the configured threshold.
   *
   * @param fileSize         current file size in bytes
   * @param lastModified     last modification time (ignored)
   * @param bytesToBeWritten bytes about to be written
   * @return {@code true} if rollover should occur
   */
  public boolean willRollover (long fileSize, long lastModified, long bytesToBeWritten) {

    return (fileSize + bytesToBeWritten) > (maxSize * fileSizeQuantifier.getMultiplier());
  }
}
