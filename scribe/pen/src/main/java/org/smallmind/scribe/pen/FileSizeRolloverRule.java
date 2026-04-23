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
 * A {@link RolloverRule} that triggers file rotation when the current file size plus the bytes
 * about to be written would exceed {@code maxSize * fileSizeQuantifier.getMultiplier()}.
 */
public class FileSizeRolloverRule implements RolloverRule {

  private FileSizeQuantifier fileSizeQuantifier;
  private long maxSize;

  /**
   * Constructs a rule with a default threshold of 10 megabytes.
   */
  public FileSizeRolloverRule () {

    this(10, FileSizeQuantifier.MEGABYTES);
  }

  /**
   * Constructs a rule with an explicit size threshold and unit quantifier.
   *
   * @param maxSize            the numeric size threshold before rollover is triggered
   * @param fileSizeQuantifier the {@link FileSizeQuantifier} whose multiplier scales {@code maxSize} to bytes
   */
  public FileSizeRolloverRule (long maxSize, FileSizeQuantifier fileSizeQuantifier) {

    super();

    this.maxSize = maxSize;
    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  /**
   * Returns the numeric size threshold that, when multiplied by the quantifier, defines the maximum file size.
   *
   * @return the configured maximum size value
   */
  public long getMaxSize () {

    return maxSize;
  }

  /**
   * Sets the numeric size threshold; the effective limit is {@code maxSize * fileSizeQuantifier.getMultiplier()} bytes.
   *
   * @param maxSize the new size threshold in units of the configured {@link FileSizeQuantifier}
   */
  public void setMaxSize (long maxSize) {

    this.maxSize = maxSize;
  }

  /**
   * Returns the quantifier that provides the byte multiplier for the size threshold.
   *
   * @return the current {@link FileSizeQuantifier}
   */
  public FileSizeQuantifier getFileSizeQuantifier () {

    return fileSizeQuantifier;
  }

  /**
   * Replaces the unit quantifier used to scale the size threshold.
   *
   * @param fileSizeQuantifier the new {@link FileSizeQuantifier}
   */
  public void setFileSizeQuantifier (FileSizeQuantifier fileSizeQuantifier) {

    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  /**
   * Returns {@code true} when {@code fileSize + bytesToBeWritten} exceeds
   * {@code maxSize * fileSizeQuantifier.getMultiplier()}, indicating that the file must be rolled
   * before the pending write.
   *
   * @param fileSize         the current size of the log file in bytes
   * @param lastModified     the last modification time in milliseconds (not used by this rule)
   * @param bytesToBeWritten the number of bytes about to be appended
   * @return {@code true} if the write would exceed the configured limit; {@code false} otherwise
   */
  public boolean willRollover (long fileSize, long lastModified, long bytesToBeWritten) {

    return (fileSize + bytesToBeWritten) > (maxSize * fileSizeQuantifier.getMultiplier());
  }
}
