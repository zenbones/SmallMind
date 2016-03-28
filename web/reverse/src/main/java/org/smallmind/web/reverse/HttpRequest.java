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

import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.http.HttpMethod;

public class HttpRequest extends HttpFrame {

  private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("([A-Z]+)\\s+(.+)\\s+HTTP/(\\d+\\.\\d+)");

  private HttpMethod method;
  private String path;

  public HttpRequest (SocketChannel sourceChannel, HttpProtocolInputStream inputStream)
    throws ProtocolException {

    this(sourceChannel, inputStream, parseRequestLine(sourceChannel, inputStream.readLine()));
  }

  private HttpRequest (SocketChannel sourceChannel, HttpProtocolInputStream inputStream, Matcher matcher)
    throws ProtocolException {

    super(sourceChannel, inputStream, matcher.group(3));

    try {
      method = HttpMethod.valueOf(matcher.group(1));
    } catch (Exception exception) {
      throw new ProtocolException(sourceChannel, CannedResponse.BAD_REQUEST);
    }

    path = matcher.group(2);
  }

  private static Matcher parseRequestLine (SocketChannel sourceChannel, String line)
    throws ProtocolException {

    Matcher matcher;

    if (!(matcher = REQUEST_LINE_PATTERN.matcher(line)).matches()) {
      throw new ProtocolException(sourceChannel, CannedResponse.BAD_REQUEST);
    }

    return matcher;
  }

  public HttpMethod getMethod () {

    return method;
  }

  public String getPath () {

    return path;
  }
}
