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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class HttpTransmitter {

  public static HttpPipe emitGetRequest (URL url, boolean openReader)
    throws IOException {

    return emitHttpRequest(HttpMethod.GET, url, openReader);
  }

  public static HttpPipe emitPutRequest (URL url, boolean openReader)
    throws IOException {

    return emitHttpRequest(HttpMethod.PUT, url, openReader);
  }

  public static HttpPipe emitPostRequest (URL url, boolean openReader)
    throws IOException {

    return emitHttpRequest(HttpMethod.POST, url, openReader);
  }

  public static HttpPipe emitDeleteRequest (URL url, boolean openReader)
    throws IOException {

    return emitHttpRequest(HttpMethod.DELETE, url, openReader);
  }

  public static HttpPipe emitHttpRequest (HttpMethod method, URL url, boolean openReader)
    throws IOException {

    HttpURLConnection urlConnection;

    urlConnection = (HttpURLConnection)url.openConnection();
    urlConnection.setDoInput(openReader);
    switch (method) {
      case GET:
        urlConnection.setDoOutput(false);
        urlConnection.setRequestMethod("GET");
        break;
      case PUT:
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("PUT");
        break;
      case POST:
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        break;
      case DELETE:
        urlConnection.setDoOutput(false);
        urlConnection.setRequestMethod("DELETE");
        break;
      default:
        throw new UnknownSwitchCaseException(method.name());
    }

    return new HttpPipe(urlConnection);
  }
}
