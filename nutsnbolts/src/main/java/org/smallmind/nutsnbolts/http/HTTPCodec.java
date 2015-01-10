/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.http;

import org.smallmind.nutsnbolts.util.Tuple;

public class HTTPCodec {

  static final String validHex = "1234567890ABCDEFabcdef";

  public static String urlEncode (Tuple<String, String> tuple, String... ignoredKeys) {

    StringBuilder dataBuilder = new StringBuilder();
    String key;

    for (int count = 0; count < tuple.size(); count++) {
      if (dataBuilder.length() > 0) {
        dataBuilder.append('&');
      }

      key = tuple.getKey(count);

      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? key : HexCodec.hexEncode(key));
      dataBuilder.append('=');
      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? tuple.getValue(count) : HexCodec.hexEncode(tuple.getValue(count)));
    }
    return dataBuilder.toString();
  }

  private static boolean isIgnoredKey (String key, String[] ignoredKeys) {

    if ((ignoredKeys != null) && (ignoredKeys.length > 0)) {
      for (String ignoredKey : ignoredKeys) {
        if (ignoredKey.equals(key)) {

          return true;
        }
      }
    }

    return false;
  }
}

