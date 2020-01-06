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
package org.smallmind.nutsnbolts.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.smallmind.nutsnbolts.util.Tuple;

public class HTTPCodec {

  public static String urlEncode (Tuple<String, String> tuple, String... ignoredKeys)
    throws UnsupportedEncodingException {

    StringBuilder dataBuilder = new StringBuilder();
    String key;

    for (int count = 0; count < tuple.size(); count++) {
      if (dataBuilder.length() > 0) {
        dataBuilder.append('&');
      }

      key = tuple.getKey(count);

      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? key : URLEncoder.encode(key, "UTF-8"));
      dataBuilder.append('=');
      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? tuple.getValue(count) : URLEncoder.encode(tuple.getValue(count), "UTF-8"));
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

  public static Tuple<String, String> urlDecode (String queryString)
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = new Tuple<>();
    StringBuilder pairBuilder = new StringBuilder();

    for (int index = 0; index < queryString.length(); index++) {

      char singleChar;

      if ((singleChar = queryString.charAt(index)) == '&') {
        decodeTuple(tuple, pairBuilder);
        pairBuilder.delete(0, pairBuilder.length());
      } else {
        pairBuilder.append(singleChar);
      }
    }

    decodeTuple(tuple, pairBuilder);

    return tuple;
  }

  private static void decodeTuple (Tuple<String, String> tuple, StringBuilder pairBuilder)
    throws UnsupportedEncodingException {

    int equalsPos;

    if ((equalsPos = pairBuilder.indexOf("=")) < 0) {
      throw new UnsupportedEncodingException("Not a standard hex encoded query string");
    }

    tuple.addPair(URLDecoder.decode(pairBuilder.substring(0, equalsPos), "UTF-8"), URLDecoder.decode(pairBuilder.substring(equalsPos + 1), "UTF-8"));
  }
}

