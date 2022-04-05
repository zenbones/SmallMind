/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.file.ephemeral;

public class EphemeralFileSystemConfiguration {

  private final String[] roots;
  private final long capacity;
  private final int blockSize;

  public EphemeralFileSystemConfiguration () {

//    this(Long.MAX_VALUE, 1024, EphemeralPath.getSeparator());
    this(Long.MAX_VALUE, 1024, "/opt/epicenter");
  }

  public EphemeralFileSystemConfiguration (long capacity, int blockSize, String... roots) {

    if ((capacity <= 0) || (blockSize <= 0)) {
      throw new IllegalArgumentException("Both capacity and block size must be > 0");
    } else if ((roots == null) || (roots.length == 0)) {
      throw new IllegalArgumentException("At least 1 root path must be specified");
    } else {

      for (String root : roots) {
        if (!root.startsWith(EphemeralPath.getSeparator())) {
          throw new IllegalArgumentException("All roots must start with " + EphemeralPath.getSeparator());
        }
      }

      this.capacity = capacity;
      this.blockSize = blockSize;
      this.roots = roots;
    }
  }

  public String[] getRoots () {

    return roots;
  }

  public long getCapacity () {

    return capacity;
  }

  public int getBlockSize () {

    return blockSize;
  }

  public boolean isOurs (String first, String... more) {

    for (String root : roots) {

      boolean matched = true;
      int position = 0;
      int match = -1;

      for (int index = 0; index < root.length(); index++) {

        String toBeMatched = (match < 0) ? first : more[match];

        while (position >= toBeMatched.length()) {
          if ((more == null) || (++match >= more.length)) {

            return false;
          } else {
            position = 0;
          }
        }

        if (root.charAt(index) != toBeMatched.charAt(position)) {
          matched = false;
          break;
        } else {
          position++;
        }
      }

      if (matched) {

        return true;
      }
    }

    return false;
  }
}

