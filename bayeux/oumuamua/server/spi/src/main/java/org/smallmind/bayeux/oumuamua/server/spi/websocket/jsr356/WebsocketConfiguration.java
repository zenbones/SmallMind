/*
 * Copyright (c) 2007 through 2024 David Berkman
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

/**
 * Configuration holder for the JSR-356 websocket transport.
 */
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

  /**
   * Creates a configuration with the mandatory endpoint class and base URL.
   *
   * @param endpointClass websocket endpoint implementation
   * @param oumuamuaUrl base URL for the Oumuamua endpoint
   */
  public WebsocketConfiguration (Class<? extends Endpoint> endpointClass, String oumuamuaUrl) {

    this.endpointClass = endpointClass;
    this.oumuamuaUrl = oumuamuaUrl;
  }

  /**
   * @return endpoint class to register
   */
  public Class<? extends Endpoint> getEndpointClass () {

    return endpointClass;
  }

  /**
   * @return configured Oumuamua URL
   */
  public String getOumuamuaUrl () {

    return oumuamuaUrl;
  }

  /**
   * @return configured websocket extensions
   */
  public Extension[] getExtensions () {

    return extensions;
  }

  /**
   * Sets extensions to register with the endpoint.
   *
   * @param extensions websocket extensions
   */
  public void setExtensions (Extension[] extensions) {

    this.extensions = extensions;
  }

  /**
   * @return negotiated subprotocol
   */
  public String getSubProtocol () {

    return subProtocol;
  }

  /**
   * Sets the subprotocol to request.
   *
   * @param subProtocol subprotocol name
   */
  public void setSubProtocol (String subProtocol) {

    this.subProtocol = subProtocol;
  }

  /**
   * @return maximum idle timeout in milliseconds (-1 for default)
   */
  public long getMaxIdleTimeoutMilliseconds () {

    return maxIdleTimeoutMilliseconds;
  }

  /**
   * Sets the maximum idle timeout.
   *
   * @param maxIdleTimeoutMilliseconds timeout in milliseconds
   */
  public void setMaxIdleTimeoutMilliseconds (long maxIdleTimeoutMilliseconds) {

    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;
  }

  /**
   * @return async send timeout in milliseconds
   */
  public long getAsyncSendTimeoutMilliseconds () {

    return asyncSendTimeoutMilliseconds;
  }

  /**
   * Sets the async send timeout.
   *
   * @param asyncSendTimeoutMilliseconds timeout in milliseconds
   */
  public void setAsyncSendTimeoutMilliseconds (long asyncSendTimeoutMilliseconds) {

    this.asyncSendTimeoutMilliseconds = asyncSendTimeoutMilliseconds;
  }

  /**
   * @return maximum text message buffer size (-1 for default)
   */
  public int getMaximumTextMessageBufferSize () {

    return maximumTextMessageBufferSize;
  }

  /**
   * Sets the maximum text message buffer size.
   *
   * @param maximumTextMessageBufferSize buffer size in characters
   */
  public void setMaximumTextMessageBufferSize (int maximumTextMessageBufferSize) {

    this.maximumTextMessageBufferSize = maximumTextMessageBufferSize;
  }
}
