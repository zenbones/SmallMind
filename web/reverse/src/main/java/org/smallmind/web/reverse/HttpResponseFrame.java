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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpResponseFrame extends HttpFrame {

  private static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile("HTTP/(\\d+\\.\\d+)\\s+(\\d+)\\s+(.+)");

  private String reason;
  private int status;

  public HttpResponseFrame (HttpProtocolInputStream httpProtocolInputStream)
    throws IOException, ProtocolException {

    this(httpProtocolInputStream, parseResponseLine(httpProtocolInputStream.readLine()));
  }

  private HttpResponseFrame (HttpProtocolInputStream inputStream, Matcher matcher)
    throws IOException, ProtocolException {

    super(inputStream, matcher.group(1));

    status = Integer.parseInt(matcher.group(2));
    reason = matcher.group(3);
  }

  private static Matcher parseResponseLine (String line)
    throws ProtocolException {

    Matcher matcher;

    if (!(matcher = RESPONSE_LINE_PATTERN.matcher(line)).matches()) {
      throw new ProtocolException(CannedResponse.BAD_REQUEST);
    }

    return matcher;
  }

  public int getStatus () {

    return status;
  }

  public String getReason () {

    return reason;
  }
}
