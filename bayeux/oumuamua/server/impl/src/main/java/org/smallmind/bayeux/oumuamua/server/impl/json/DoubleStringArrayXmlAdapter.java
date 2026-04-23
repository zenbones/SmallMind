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
package org.smallmind.bayeux.oumuamua.server.impl.json;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB {@link XmlAdapter} that marshals a {@code String[][]} to a bracketed list of
 * slash-joined path strings (e.g. {@code [/a/b,/c/d]}); unmarshalling is intentionally
 * unsupported.
 */
public class DoubleStringArrayXmlAdapter extends XmlAdapter<String, String[][]> {

  /**
   * Not supported; this adapter is marshal-only.
   *
   * @param s the serialized string value (unused)
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  @Override
  public String[][] unmarshal (String s) {

    throw new UnsupportedOperationException();
  }

  /**
   * Serializes a two-dimensional string array into a bracketed list of slash-prefixed,
   * slash-joined paths (e.g. inner array {@code ["a","b"]} becomes {@code /a/b}).
   *
   * @param doubleArray the array of path-segment arrays to serialize
   * @return the bracketed path-list string, or {@code null} if {@code doubleArray} is
   * {@code null}
   */
  @Override
  public String marshal (String[][] doubleArray) {

    if (doubleArray == null) {

      return null;
    } else {

      StringBuilder builder = new StringBuilder("[");
      boolean first = true;

      for (String[] array : doubleArray) {
        if (!first) {
          builder.append(",");
        }

        builder.append("/").append(String.join("/", array));
        first = false;
      }

      return builder.append("]").toString();
    }
  }
}
