/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.memcached.cubby;

public class TokenGenerator {

  private static final String ALPHABET = "!#$%&()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{|}~";

  private final byte[] counter = new byte[32];

  public synchronized String next () {

    StringBuilder countBuilder = new StringBuilder();
    int index = 0;

    do {
      if (counter[index] < ALPHABET.length()) {
        counter[index] = (byte)(counter[index] + 1);
        break;
      } else {
        counter[index] = 1;
      }
    } while (++index < 32);

    if (index == 32) {
      counter[0] = 1;
      for (int column = 1; column < 32; column++) {
        counter[column] = 0;
      }
    }

    for (int loop = 0; loop < 32; loop++) {
      if (counter[loop] > 0) {
        countBuilder.append(ALPHABET.charAt(counter[loop] - 1));
      } else {
        break;
      }
    }

    return countBuilder.toString();
  }
}
