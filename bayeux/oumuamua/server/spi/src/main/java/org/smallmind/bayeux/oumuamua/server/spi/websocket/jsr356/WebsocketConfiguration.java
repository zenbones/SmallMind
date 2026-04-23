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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;

/**
 * Value object that aggregates all configuration parameters required to register and tune the
 * JSR-356 {@link WebSocketTransport} endpoint, including the endpoint class, mount URL,
 * subprotocol, extensions, and timeout/buffer size overrides.
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
   * Creates a configuration with the two mandatory parameters; all optional properties default to
   * their commented values declared on the fields.
   *
   * @param endpointClass the {@link Endpoint} subclass to register with the servlet container
   * @param oumuamuaUrl   the URL path under which the Oumuamua Bayeux endpoint will be mounted;
   *                      leading slashes and trailing {@code /*} wildcards are normalised at
   *                      registration time
   */
  public WebsocketConfiguration (Class<? extends Endpoint> endpointClass, String oumuamuaUrl) {

    this.endpointClass = endpointClass;
    this.oumuamuaUrl = oumuamuaUrl;
  }

  /**
   * Returns the {@link Endpoint} subclass that will be registered with the JSR-356 container.
   *
   * @return endpoint implementation class
   */
  public Class<? extends Endpoint> getEndpointClass () {

    return endpointClass;
  }

  /**
   * Returns the raw URL path configured for the Oumuamua endpoint before normalisation.
   *
   * @return URL path string as provided to the constructor
   */
  public String getOumuamuaUrl () {

    return oumuamuaUrl;
  }

  /**
   * Returns the websocket extensions to advertise during endpoint registration, or {@code null}
   * if none have been configured.
   *
   * @return array of {@link Extension} instances, or {@code null}
   */
  public Extension[] getExtensions () {

    return extensions;
  }

  /**
   * Sets the websocket extensions to register with the endpoint.
   *
   * @param extensions array of {@link Extension} instances to advertise; {@code null} means none
   */
  public void setExtensions (Extension[] extensions) {

    this.extensions = extensions;
  }

  /**
   * Returns the subprotocol name to include in the endpoint configuration, or {@code null} if none
   * has been set.
   *
   * @return subprotocol string, or {@code null}
   */
  public String getSubProtocol () {

    return subProtocol;
  }

  /**
   * Sets the subprotocol name to negotiate with the client.
   *
   * @param subProtocol subprotocol identifier string, or {@code null} to omit subprotocol negotiation
   */
  public void setSubProtocol (String subProtocol) {

    this.subProtocol = subProtocol;
  }

  /**
   * Returns the maximum time a session may remain idle before the container closes it.
   *
   * @return idle timeout in milliseconds, or {@code -1} to use the container default
   */
  public long getMaxIdleTimeoutMilliseconds () {

    return maxIdleTimeoutMilliseconds;
  }

  /**
   * Overrides the container's default idle session timeout.
   *
   * @param maxIdleTimeoutMilliseconds idle timeout in milliseconds; use {@code -1} to keep the
   *                                   container default
   */
  public void setMaxIdleTimeoutMilliseconds (long maxIdleTimeoutMilliseconds) {

    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;
  }

  /**
   * Returns the timeout applied to asynchronous message sends.
   *
   * @return async send timeout in milliseconds; {@code 0} means no timeout
   */
  public long getAsyncSendTimeoutMilliseconds () {

    return asyncSendTimeoutMilliseconds;
  }

  /**
   * Sets the timeout applied to asynchronous message sends.
   *
   * @param asyncSendTimeoutMilliseconds timeout in milliseconds; {@code 0} disables the timeout
   */
  public void setAsyncSendTimeoutMilliseconds (long asyncSendTimeoutMilliseconds) {

    this.asyncSendTimeoutMilliseconds = asyncSendTimeoutMilliseconds;
  }

  /**
   * Returns the maximum number of characters the container will buffer for a single incoming text
   * message before failing the connection.
   *
   * @return buffer size in characters, or {@code -1} to use the container default
   */
  public int getMaximumTextMessageBufferSize () {

    return maximumTextMessageBufferSize;
  }

  /**
   * Overrides the container's default incoming text message buffer size.
   *
   * @param maximumTextMessageBufferSize buffer size in characters; use {@code -1} to keep the
   *                                     container default
   */
  public void setMaximumTextMessageBufferSize (int maximumTextMessageBufferSize) {

    this.maximumTextMessageBufferSize = maximumTextMessageBufferSize;
  }
}
