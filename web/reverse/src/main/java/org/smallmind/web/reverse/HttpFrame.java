/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.web.reverse;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class HttpFrame {

  private final List<HttpHeader> headerList;
  private final String version;

  public HttpFrame (HttpProtocolInputStream httpProtocolInputStream, String version)
    throws IOException, ProtocolException {

    this.version = version;

    headerList = parseHeaders(httpProtocolInputStream);
  }

  public abstract HttpDirection getDirection ();

  private List<HttpHeader> parseHeaders (HttpProtocolInputStream httpProtocolInputStream)
    throws IOException, ProtocolException {

    LinkedHashMap<String, HttpHeader> headerMap = new LinkedHashMap<>();
    String line;

    while (!(line = httpProtocolInputStream.readLine()).isEmpty()) {

      HttpHeader header;
      String normalizeName;
      int colonPos;

      if ((colonPos = line.indexOf(':')) < 0) {
        throw new ProtocolException(CannedResponse.BAD_REQUEST);
      }
      if ((header = headerMap.get(normalizeName = normalizeHeaderName(line.substring(0, colonPos).trim()))) == null) {
        headerMap.put(normalizeName, header = new HttpHeader(normalizeName));
      }

      header.addValue(line.substring(colonPos + 1).trim());
    }

    return new LinkedList<>(headerMap.values());
  }

  private String normalizeHeaderName (String name) {

    StringBuilder normalizedBuilder = new StringBuilder();
    boolean upperCase = true;

    for (int index = 0; index < name.length(); index++) {

      char singleChar = name.charAt(index);

      if (singleChar == '-') {
        normalizedBuilder.append('-');
        upperCase = true;
      } else if (upperCase) {
        normalizedBuilder.append(Character.toUpperCase(singleChar));
        upperCase = false;
      } else {
        normalizedBuilder.append(singleChar);
      }
    }

    return normalizedBuilder.toString();
  }

  public String getVersion () {

    return version;
  }

  public HttpHeader getHeader (String name) {

    String normalizedName = normalizeHeaderName(name.trim());

    for (HttpHeader header : headerList) {
      if (header.getName().equals(normalizedName)) {

        return header;
      }
    }

    return null;
  }

  public void addHeader (HttpHeader header) {

    addHeader(header, false);
  }

  public void addHeader (HttpHeader header, boolean prepend) {

    if (prepend) {
      headerList.add(0, header);
    } else {
      headerList.add(header);
    }
  }

  public HttpHeader removeHeader (String name) {

    Iterator<HttpHeader> headerIter = headerList.iterator();
    String normalizedName = normalizeHeaderName(name.trim());

    while (headerIter.hasNext()) {

      HttpHeader header;

      if ((header = headerIter.next()).getName().equals(normalizedName)) {
        headerIter.remove();

        return header;
      }
    }

    return null;
  }

  public List<HttpHeader> getHeaders () {

    return headerList;
  }
}
