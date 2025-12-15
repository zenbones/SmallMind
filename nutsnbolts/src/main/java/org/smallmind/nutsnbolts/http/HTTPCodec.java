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
package org.smallmind.nutsnbolts.http;

import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.util.Tuple;

/**
 * Utility for encoding Tuples as query strings and parsing query strings back to Tuples.
 */
public class HTTPCodec {

  /**
   * Builds an application/x-www-form-urlencoded query string from a tuple.
   *
   * @param tuple       ordered key/value pairs to encode
   * @param ignoredKeys optional keys that should be emitted verbatim without encoding
   * @return encoded query string
   * @throws UnsupportedEncodingException if encoding fails
   */
  public static String urlEncode (Tuple<String, String> tuple, String... ignoredKeys)
    throws UnsupportedEncodingException {

    StringBuilder dataBuilder = new StringBuilder();
    String key;

    for (int count = 0; count < tuple.size(); count++) {
      if (dataBuilder.length() > 0) {
        dataBuilder.append('&');
      }

      key = tuple.getKey(count);

      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? key : URLCodec.urlEncode(key));
      dataBuilder.append('=');
      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? tuple.getValue(count) : URLCodec.urlEncode(tuple.getValue(count)));
    }
    return dataBuilder.toString();
  }

  /**
   * Determines whether a tuple key should bypass encoding.
   *
   * @param key         key to test
   * @param ignoredKeys optional array of unencoded keys
   * @return {@code true} if the key should be emitted verbatim
   */
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

  /**
   * Parses an application/x-www-form-urlencoded query string into a tuple.
   *
   * @param queryString query portion of a URL
   * @return tuple containing decoded keys and values (in order)
   * @throws UnsupportedEncodingException if the input is not valid percent-encoding
   */
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

  /**
   * Decodes an individual {@code key=value} pair and appends it to the tuple.
   *
   * @param tuple       destination tuple
   * @param pairBuilder buffer containing the pair
   * @throws UnsupportedEncodingException if the pair is malformed or not encoded
   */
  private static void decodeTuple (Tuple<String, String> tuple, StringBuilder pairBuilder)
    throws UnsupportedEncodingException {

    int equalsPos;

    if ((equalsPos = pairBuilder.indexOf("=")) < 0) {
      throw new UnsupportedEncodingException("Not a standard hex encoded query string");
    }

    tuple.addPair(URLCodec.urlDecode(pairBuilder.substring(0, equalsPos)), URLCodec.urlDecode(pairBuilder.substring(equalsPos + 1)));
  }
}
