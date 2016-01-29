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
import java.net.InetSocketAddress;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.smallmind.scribe.pen.LoggerManager;

public class ReverseProxy {

  private final ConnectingIOReactorWorker connectingIOReactorWorker;
  private final ListeningIOReactorWorker listeningIOReactorWorker;
  private final HttpHostDictionary httpHostDictionary;
  private final int proxyPort;
  private final int concurrencyLevel;

  public ReverseProxy (int proxyPort, int concurrencyLevel, HttpHostDictionary httpHostDictionary)
    throws IOReactorException {

    this.proxyPort = proxyPort;
    this.concurrencyLevel = concurrencyLevel;
    this.httpHostDictionary = httpHostDictionary;

    new Thread(connectingIOReactorWorker = new ConnectingIOReactorWorker()).start();
    new Thread(listeningIOReactorWorker = new ListeningIOReactorWorker(connectingIOReactorWorker.getConnectingIOReactor())).start();
  }

  public void shutdown () {

    try {
      listeningIOReactorWorker.stop();
    } catch (IOException ioException) {
      LoggerManager.getLogger(ReverseProxy.class).error(ioException);
    }
    try {
      connectingIOReactorWorker.stop();
    } catch (IOException ioException) {
      LoggerManager.getLogger(ReverseProxy.class).error(ioException);
    }
  }

  private class ConnectingIOReactorWorker implements Runnable {

    private final ConnectingIOReactor connectingIOReactor;

    public ConnectingIOReactorWorker ()
      throws IOReactorException {

      connectingIOReactor = new DefaultConnectingIOReactor(IOReactorConfig.custom().setIoThreadCount(1).setSoTimeout(3000).setConnectTimeout(3000).build());
    }

    public ConnectingIOReactor getConnectingIOReactor () {

      return connectingIOReactor;
    }

    public void stop ()
      throws IOException {

      connectingIOReactor.shutdown();
    }

    @Override
    public void run () {

      try {
        connectingIOReactor.execute(new DefaultHttpClientIODispatch(new ProxyClientProtocolHandler(), ConnectionConfig.DEFAULT));
      } catch (Exception exception) {
        LoggerManager.getLogger(ConnectingIOReactorWorker.class).error(exception);
      } finally {
        ReverseProxy.this.shutdown();
      }
    }
  }

  private class ListeningIOReactorWorker implements Runnable {

    private final ListeningIOReactor listeningIOReactor;
    private final RequestHandlerMapper requestHandlerMapper;

    public ListeningIOReactorWorker (ConnectingIOReactor connectingIOReactor)
      throws IOReactorException {

      ProxyConnPool proxyConnPool = new ProxyConnPool(connectingIOReactor, ConnectionConfig.DEFAULT);

      proxyConnPool.setMaxTotal(concurrencyLevel);
      proxyConnPool.setDefaultMaxPerRoute(concurrencyLevel);

      listeningIOReactor = new DefaultListeningIOReactor(IOReactorConfig.custom().setIoThreadCount(1).setSoTimeout(3000).setConnectTimeout(3000).build());
      requestHandlerMapper = new RequestHandlerMapper(httpHostDictionary, new HttpAsyncRequester(new ImmutableHttpProcessor(new ResponseDate(), new ResponseServer("Test/1.1"), new ResponseContent(), new ResponseConnControl()), new ProxyOutgoingConnectionReuseStrategy()), proxyConnPool);
    }

    public void stop ()
      throws IOException {

      listeningIOReactor.shutdown();
    }

    @Override
    public void run () {

      try {
        listeningIOReactor.listen(new InetSocketAddress(proxyPort));
        listeningIOReactor.execute(new DefaultHttpServerIODispatch(new ProxyServiceHandler(new ImmutableHttpProcessor(new ResponseDate(), new ResponseServer("Test/1.1"), new ResponseContent(), new ResponseConnControl()), new ProxyIncomingConnectionReuseStrategy(), requestHandlerMapper), ConnectionConfig.DEFAULT));
      } catch (Exception exception) {
        LoggerManager.getLogger(ListeningIOReactorWorker.class).error(exception);
      } finally {
        ReverseProxy.this.shutdown();
      }
    }
  }
}