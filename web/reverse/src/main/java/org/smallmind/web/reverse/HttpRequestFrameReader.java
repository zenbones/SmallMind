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
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public class HttpRequestFrameReader extends HttpFrameReader {

  private final AtomicReference<SocketChannel> destinationChannelRef = new AtomicReference<>();
  private int connectTimeoutMillis;

  public HttpRequestFrameReader (ReverseProxyService reverseProxyService, SocketChannel sourceChannel, int connectTimeoutMillis) {

    super(reverseProxyService, sourceChannel);

    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  @Override
  public void closeChannels (SocketChannel sourceChannel) {

    try {
      sourceChannel.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }

    try {

      SocketChannel destinationChannel;

      if ((destinationChannel = destinationChannelRef.get()) != null) {
        destinationChannel.close();
      }
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }
  }

  public void registerDestination (SocketChannel destinationChannel) {

    synchronized (destinationChannelRef) {
      destinationChannelRef.set(destinationChannel);
      destinationChannelRef.notify();
    }
  }

  @Override
  public SocketChannel getTargetChannel (SocketChannel sourceChannel) {

    SocketChannel destinationChannel = null;
    long start = System.currentTimeMillis();
    long elapsed;

    try {
      while (((elapsed = System.currentTimeMillis() - start) < connectTimeoutMillis) && ((destinationChannel = destinationChannelRef.get()) == null)) {
        synchronized (destinationChannelRef) {
          destinationChannelRef.wait(connectTimeoutMillis - elapsed);
        }
      }
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(interruptedException);
    }

    return destinationChannel;
  }

  @Override
  public HttpRequestFrame getHttpFrame (ReverseProxyService reverseProxyService, SocketChannel sourceSocketChannel, HttpProtocolInputStream httpProtocolInputStream)
    throws ProtocolException {

    HttpRequestFrame httpRequestFrame = new HttpRequestFrame(httpProtocolInputStream);
    ProxyTarget proxyTarget = reverseProxyService.connectDestination(sourceSocketChannel, this, httpRequestFrame);
    HttpHeader hostHeader;

    if ((hostHeader = httpRequestFrame.getHeader("Host")) != null) {
      hostHeader.setValue(proxyTarget.getHost() + ":" + proxyTarget.getPort());
    } else {
      httpRequestFrame.addHeader(new HttpHeader("Host").addValue(proxyTarget.getHost() + ":" + proxyTarget.getPort()));
    }

    clearBuffer();
    writeToBuffer(httpRequestFrame.toByteArray());

    return httpRequestFrame;
  }
}
