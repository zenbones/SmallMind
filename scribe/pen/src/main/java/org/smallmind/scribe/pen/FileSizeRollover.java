/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import java.io.File;

public class FileSizeRollover extends Rollover {

  private FileSizeQuantifier fileSizeQuantifier;
  private long maxSize;

  public FileSizeRollover () {

    this(10, FileSizeQuantifier.MEGABYTES);
  }

  public FileSizeRollover (long maxSize, FileSizeQuantifier fileSizeQuantifier) {

    super();

    this.maxSize = maxSize;
    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  public FileSizeRollover (long maxSize, FileSizeQuantifier fileSizeQuantifier, char separator, Timestamp timestamp) {

    super(separator, timestamp);

    this.maxSize = maxSize;
    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  public long getMaxSize () {

    return maxSize;
  }

  public void setMaxSize (long maxSize) {

    this.maxSize = maxSize;
  }

  public FileSizeQuantifier getFileSizeQuantifier () {

    return fileSizeQuantifier;
  }

  public void setFileSizeQuantifier (FileSizeQuantifier fileSizeQuantifier) {

    this.fileSizeQuantifier = fileSizeQuantifier;
  }

  public boolean willRollover (File logFile, long bytesToBeWritten) {

    return (logFile.length() + bytesToBeWritten) > (maxSize * fileSizeQuantifier.getMultiplier());
  }
}