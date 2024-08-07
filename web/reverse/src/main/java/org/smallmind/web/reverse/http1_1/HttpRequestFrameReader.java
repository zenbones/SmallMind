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
package org.smallmind.web.reverse.http1_1;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public class HttpRequestFrameReader extends HttpFrameReader {

  private final AtomicReference<DestinationTicket> destinationTicketRef = new AtomicReference<>();
  private final int connectTimeoutMillis;

  public HttpRequestFrameReader (ReverseProxyService reverseProxyService, SocketChannel sourceChannel, int connectTimeoutMillis) {

    super(reverseProxyService, sourceChannel);

    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  @Override
  public void closeChannels () {

    try {
      getSourceChannel().close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }

    try {

      DestinationTicket destinationTicket;

      if ((destinationTicket = destinationTicketRef.get()) != null) {
        destinationTicket.getSocketChannel().close();
      }
    } catch (IOException ioException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
    }
  }

  public void registerDestination (ProxyTarget target, SocketChannel destinationChannel) {

    synchronized (destinationTicketRef) {
      destinationTicketRef.set(new DestinationTicket(target, destinationChannel));
      destinationTicketRef.notify();
    }
  }

  @Override
  public SocketChannel getTargetChannel () {

    DestinationTicket destinationTicket = null;
    long start = System.currentTimeMillis();
    long elapsed;

    try {
      while (((elapsed = System.currentTimeMillis() - start) < connectTimeoutMillis) && ((destinationTicket = destinationTicketRef.get()) == null)) {
        synchronized (destinationTicketRef) {
          destinationTicketRef.wait(connectTimeoutMillis - elapsed);
        }
      }
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(HttpRequestFrameReader.class).error(interruptedException);
    }

    return (destinationTicket == null) ? null : destinationTicket.getSocketChannel();
  }

  @Override
  public SocketChannel getDestinationChannel ()
    throws ProtocolException {

    DestinationTicket destinationTicket;

    if ((destinationTicket = destinationTicketRef.get()) == null) {
      throw new ProtocolException(CannedResponse.BAD_GATEWAY);
    }

    return destinationTicket.getSocketChannel();
  }

  @Override
  public HttpRequestFrame getHttpFrame (ReverseProxyService reverseProxyService, SocketChannel sourceSocketChannel, HttpProtocolInputStream httpProtocolInputStream, HttpProtocolOutputStream httpProtocolOutputStream)
    throws IOException, ProtocolException {

    HttpRequestFrame httpRequestFrame = new HttpRequestFrame(httpProtocolInputStream);
    ProxyTarget proxyTarget = reverseProxyService.lookup(httpRequestFrame);
    DestinationTicket destinationTicket;
    HttpHeader hostHeader;

    if (((destinationTicket = destinationTicketRef.get()) == null) || (!destinationTicket.getProxyTarget().equals(proxyTarget)) || (!destinationTicket.getSocketChannel().isOpen())) {
      if (destinationTicket != null) {
        try {
          destinationTicket.getSocketChannel().close();
        } catch (IOException ioException) {
          LoggerManager.getLogger(HttpRequestFrameReader.class).error(ioException);
        }

        destinationTicketRef.compareAndSet(destinationTicket, null);
      }

      reverseProxyService.connectDestination(sourceSocketChannel, this, proxyTarget);
    }

    if ((hostHeader = httpRequestFrame.getHeader("Host")) != null) {
      hostHeader.setValue(proxyTarget.getHost() + ":" + proxyTarget.getPort());
    } else {
      httpRequestFrame.addHeader(new HttpHeader("Host").addValue(proxyTarget.getHost() + ":" + proxyTarget.getPort()));
    }

    httpRequestFrame.toOutputStream(httpProtocolOutputStream);

    return httpRequestFrame;
  }
}
