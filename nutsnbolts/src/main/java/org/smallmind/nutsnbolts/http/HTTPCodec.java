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
 * Encodes key/value tuples as {@code application/x-www-form-urlencoded} query strings and decodes them back.
 */
public class HTTPCodec {

  /**
   * Serializes a tuple of key/value pairs as a URL-encoded query string, optionally leaving specified keys unencoded.
   *
   * @param tuple       ordered key/value pairs to encode
   * @param ignoredKeys keys whose names and values are emitted verbatim without percent-encoding
   * @return {@code application/x-www-form-urlencoded} query string
   * @throws UnsupportedEncodingException if URL encoding of any key or value fails
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
   * Returns {@code true} when the given key appears in the list of keys to leave unencoded.
   *
   * @param key         key to test
   * @param ignoredKeys array of keys that must not be encoded; may be {@code null} or empty
   * @return {@code true} if {@code key} is in {@code ignoredKeys}
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
   * Parses an {@code application/x-www-form-urlencoded} query string and returns all key/value pairs as a tuple.
   *
   * @param queryString the query string to decode (without a leading {@code ?})
   * @return ordered tuple of decoded key/value pairs
   * @throws UnsupportedEncodingException if the string is not valid percent-encoded form data
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
   * Decodes a single {@code key=value} segment and appends the pair to the accumulating tuple.
   *
   * @param tuple       tuple to receive the decoded pair
   * @param pairBuilder buffer holding exactly one {@code key=value} segment
   * @throws UnsupportedEncodingException if the segment does not contain {@code =} or the encoding is invalid
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
