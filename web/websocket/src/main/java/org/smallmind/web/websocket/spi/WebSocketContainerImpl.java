/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.websocket.spi;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.smallmind.web.websocket.WebSocketException;

public class WebSocketContainerImpl implements WebSocketContainer {

  private final AtomicLong defaultMaxSessionIdleTimeout = new AtomicLong(-1);
  private final AtomicInteger defaultMaxTextMessageBufferSize = new AtomicInteger(Integer.MAX_VALUE);
  private final AtomicInteger defaultMaxBinaryMessageBufferSize = new AtomicInteger(Integer.MAX_VALUE);
  private AtomicLong defaultAsyncSendTimeout = new AtomicLong(-1);

  @Override
  public Session connectToServer (Class<?> annotatedEndpointClass, URI path)
    throws DeploymentException, IOException {

    AnnotatedEndpoint annotatedEndpoint = new AnnotatedEndpoint(annotatedEndpointClass);

    try {
      return new SessionImpl(this, path, annotatedEndpoint, annotatedEndpoint.getEndpointConfig());
    } catch (NoSuchAlgorithmException | WebSocketException exception) {
      throw new DeploymentException("Unable to instantiate a connection", exception);
    }
  }

  @Override
  public Session connectToServer (Object annotatedEndpointInstance, URI path)
    throws DeploymentException, IOException {

    AnnotatedEndpoint annotatedEndpoint = new AnnotatedEndpoint(annotatedEndpointInstance.getClass());

    try {
      return new SessionImpl(this, path, annotatedEndpoint, annotatedEndpoint.getEndpointConfig());
    } catch (NoSuchAlgorithmException | WebSocketException exception) {
      throw new DeploymentException("Unable to instantiate a connection", exception);
    }
  }

  @Override
  public Session connectToServer (Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path)
    throws DeploymentException, IOException {

    try {
      return new SessionImpl(this, path, endpointClass.newInstance(), cec);
    } catch (IllegalAccessException | InstantiationException | NoSuchAlgorithmException | WebSocketException exception) {
      throw new DeploymentException("Unable to instantiate a connection", exception);
    }
  }

  @Override
  public Session connectToServer (Endpoint endpointInstance, ClientEndpointConfig cec, URI path)
    throws DeploymentException, IOException {

    try {
      return new SessionImpl(this, path, endpointInstance, cec);
    } catch (NoSuchAlgorithmException | WebSocketException exception) {
      throw new DeploymentException("Unable to instantiate a connection", exception);
    }
  }

  @Override
  public long getDefaultAsyncSendTimeout () {

    return defaultAsyncSendTimeout.get();
  }

  @Override
  public void setAsyncSendTimeout (long timeoutmillis) {

    defaultAsyncSendTimeout.set(timeoutmillis);
  }

  @Override
  public long getDefaultMaxSessionIdleTimeout () {

    return defaultMaxSessionIdleTimeout.get();
  }

  @Override
  public void setDefaultMaxSessionIdleTimeout (long timeout) {

    defaultMaxSessionIdleTimeout.set(timeout);
  }

  @Override
  public int getDefaultMaxBinaryMessageBufferSize () {

    return defaultMaxBinaryMessageBufferSize.get();
  }

  @Override
  public void setDefaultMaxBinaryMessageBufferSize (int max) {

    defaultMaxBinaryMessageBufferSize.set(max);
  }

  @Override
  public int getDefaultMaxTextMessageBufferSize () {

    return defaultMaxTextMessageBufferSize.get();
  }

  @Override
  public void setDefaultMaxTextMessageBufferSize (int max) {

    defaultMaxTextMessageBufferSize.set(max);
  }

  @Override
  public Set<Extension> getInstalledExtensions () {

    return null;
  }
}
