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

import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.HttpRequest;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
import org.apache.http.nio.protocol.HttpAsyncRequester;

public class RequestHandlerMapper implements HttpAsyncRequestHandlerMapper {

  private final ConcurrentHashMap<HttpHostKey, ProxyRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
  private final HttpHostDictionary httpHostDictionary;
  private final HttpAsyncRequester executor;
  private final BasicNIOConnPool connPool;

  public RequestHandlerMapper (HttpHostDictionary httpHostDictionary, HttpAsyncRequester executor, BasicNIOConnPool connPool) {

    this.httpHostDictionary = httpHostDictionary;
    this.executor = executor;
    this.connPool = connPool;
  }

  @Override
  public HttpAsyncRequestHandler<?> lookup (HttpRequest httpRequest) {

    ProxyRequestHandler proxyRequestHandler;
    HttpHostEntry httpHostEntry;

    if ((httpHostEntry = httpHostDictionary.lookup(httpRequest)) == null) {
      throw new ReverseProxyException("Unable to translate request(%s) into a proxy destination", httpRequest.getRequestLine().getUri());
    }

    if ((proxyRequestHandler = requestHandlerMap.get(httpHostEntry.getHttpHostKey())) == null) {
      synchronized (requestHandlerMap) {
        if ((proxyRequestHandler = requestHandlerMap.get(httpHostEntry.getHttpHostKey())) == null) {
          requestHandlerMap.put(httpHostEntry.getHttpHostKey(), proxyRequestHandler = new ProxyRequestHandler(httpHostEntry.getHttpHost(), executor, connPool));
        }
      }
    }

    return proxyRequestHandler;
  }
}
