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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;

public class WebsocketConfiguration {

  private final Class<? extends Endpoint> endpointClass;
  private final String oumuamuaUrl;

  private Extension[] extensions;
  private String subProtocol;
  // Use the default;
  private long maxIdleTimeoutMilliseconds = -1;
  // No timeout
  private long asyncSendTimeoutMilliseconds = 0;
  // Use the default
  private int maximumTextMessageBufferSize = -1;

  public WebsocketConfiguration (Class<? extends Endpoint> endpointClass, String oumuamuaUrl) {

    this.endpointClass = endpointClass;
    this.oumuamuaUrl = oumuamuaUrl;
  }

  public Class<? extends Endpoint> getEndpointClass () {

    return endpointClass;
  }

  public String getOumuamuaUrl () {

    return oumuamuaUrl;
  }

  public Extension[] getExtensions () {

    return extensions;
  }

  public void setExtensions (Extension[] extensions) {

    this.extensions = extensions;
  }

  public String getSubProtocol () {

    return subProtocol;
  }

  public void setSubProtocol (String subProtocol) {

    this.subProtocol = subProtocol;
  }

  public long getMaxIdleTimeoutMilliseconds () {

    return maxIdleTimeoutMilliseconds;
  }

  public void setMaxIdleTimeoutMilliseconds (long maxIdleTimeoutMilliseconds) {

    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;
  }

  public long getAsyncSendTimeoutMilliseconds () {

    return asyncSendTimeoutMilliseconds;
  }

  public void setAsyncSendTimeoutMilliseconds (long asyncSendTimeoutMilliseconds) {

    this.asyncSendTimeoutMilliseconds = asyncSendTimeoutMilliseconds;
  }

  public int getMaximumTextMessageBufferSize () {

    return maximumTextMessageBufferSize;
  }

  public void setMaximumTextMessageBufferSize (int maximumTextMessageBufferSize) {

    this.maximumTextMessageBufferSize = maximumTextMessageBufferSize;
  }
}
