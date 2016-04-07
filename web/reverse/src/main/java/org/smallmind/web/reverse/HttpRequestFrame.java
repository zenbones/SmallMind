/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.http.HttpMethod;

public class HttpRequestFrame extends HttpFrame {

  private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("([A-Z]+)\\s+(.+)\\s+HTTP/(\\d+\\.\\d+)");

  private HttpMethod method;
  private String path;

  public HttpRequestFrame (HttpProtocolInputStream httpProtocolInputStream)
    throws IOException, ProtocolException {

    this(httpProtocolInputStream, parseRequestLine(httpProtocolInputStream.readLine()));
  }

  private HttpRequestFrame (HttpProtocolInputStream inputStream, Matcher matcher)
    throws IOException, ProtocolException {

    super(inputStream, matcher.group(3));

    try {
      method = HttpMethod.valueOf(matcher.group(1));
    } catch (Exception exception) {
      throw new ProtocolException(CannedResponse.BAD_REQUEST);
    }

    path = matcher.group(2);
  }

  private static Matcher parseRequestLine (String line)
    throws ProtocolException {

    Matcher matcher;

    if (!(matcher = REQUEST_LINE_PATTERN.matcher(line)).matches()) {
      throw new ProtocolException(CannedResponse.BAD_REQUEST);
    }

    return matcher;
  }

  @Override
  public HttpDirection getDirection () {

    return HttpDirection.REQUEST;
  }

  public HttpMethod getMethod () {

    return method;
  }

  public String getPath () {

    return path;
  }

  public void toOutputStream (OutputStream outputStream)
    throws IOException {

    outputStream.write(getMethod().name().getBytes());
    outputStream.write(' ');
    outputStream.write(getPath().getBytes());
    outputStream.write(" HTTP/".getBytes());
    outputStream.write(getVersion().getBytes());
    outputStream.write("\r\n".getBytes());

    for (HttpHeader header : getHeaders()) {

      int valueIndex = 0;

      outputStream.write(header.getName().getBytes());
      outputStream.write(": ".getBytes());
      for (String value : header.getValues()) {
        if (valueIndex++ > 0) {
          outputStream.write(", ".getBytes());
        }
        outputStream.write(value.getBytes());
      }
      outputStream.write("\r\n".getBytes());
    }
    outputStream.write("\r\n".getBytes());
  }
}
