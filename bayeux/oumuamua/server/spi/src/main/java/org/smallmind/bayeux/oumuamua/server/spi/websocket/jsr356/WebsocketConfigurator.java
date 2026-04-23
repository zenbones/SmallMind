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

import java.util.List;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * {@link ServerEndpointConfig.Configurator} decorator that forwards every negotiation callback to a
 * wrapped delegate, providing a seam for future interception without coupling callers to a concrete
 * configurator type.
 */
public class WebsocketConfigurator extends ServerEndpointConfig.Configurator {

  private final ServerEndpointConfig.Configurator internal;

  /**
   * Constructs a decorator around the supplied delegate.
   *
   * @param internal the {@link ServerEndpointConfig.Configurator} to which all calls are forwarded
   */
  public WebsocketConfigurator (ServerEndpointConfig.Configurator internal) {

    this.internal = internal;
  }

  /**
   * Delegates subprotocol selection to the wrapped configurator.
   *
   * @param supported subprotocols the server endpoint declares support for
   * @param requested subprotocols the client offered during the upgrade handshake
   * @return the subprotocol chosen by the delegate, or an empty string if none is acceptable
   */
  @Override
  public String getNegotiatedSubprotocol (List<String> supported, List<String> requested) {

    return internal.getNegotiatedSubprotocol(supported, requested);
  }

  /**
   * Delegates websocket extension negotiation to the wrapped configurator.
   *
   * @param installed extensions installed and available on the server
   * @param requested extensions the client requested during the upgrade handshake
   * @return the list of extensions the delegate chooses to enable for the session
   */
  @Override
  public List<Extension> getNegotiatedExtensions (List<Extension> installed, List<Extension> requested) {

    return internal.getNegotiatedExtensions(installed, requested);
  }

  /**
   * Delegates origin validation to the wrapped configurator.
   *
   * @param originHeaderValue value of the HTTP {@code Origin} header sent by the client
   * @return {@code true} if the delegate permits the connection from the given origin
   */
  @Override
  public boolean checkOrigin (String originHeaderValue) {

    return internal.checkOrigin(originHeaderValue);
  }

  /**
   * Delegates HTTP upgrade handshake modification to the wrapped configurator, allowing it to
   * inspect request headers and mutate response headers before the handshake completes.
   *
   * @param serverEndpointConfig the endpoint configuration for the session being opened
   * @param request              the incoming HTTP upgrade request
   * @param response             the outgoing HTTP upgrade response to modify
   */
  @Override
  public void modifyHandshake (ServerEndpointConfig serverEndpointConfig, HandshakeRequest request, HandshakeResponse response) {

    internal.modifyHandshake(serverEndpointConfig, request, response);
  }

  /**
   * Delegates endpoint instance creation to the wrapped configurator and casts the result to the
   * requested type.
   *
   * @param <T>           the endpoint type
   * @param endpointClass the class of endpoint to instantiate
   * @return a new endpoint instance of type {@code T} produced by the delegate
   * @throws InstantiationException if the delegate cannot construct the endpoint instance
   */
  @Override
  public <T> T getEndpointInstance (Class<T> endpointClass)
    throws InstantiationException {

    return endpointClass.cast(internal.getEndpointInstance(endpointClass));
  }
}
