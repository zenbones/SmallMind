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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.protocol.HttpContext;
import org.smallmind.scribe.pen.LoggerManager;

public class ProxyRequestHandler implements HttpAsyncRequestHandler<ProxyHttpExchange> {

  private final HttpHost target;
  private final HttpAsyncRequester executor;
  private final BasicNIOConnPool connPool;
  private final AtomicLong counter;

  public ProxyRequestHandler (final HttpHost target, final HttpAsyncRequester executor, final BasicNIOConnPool connPool) {

    super();
    this.target = target;
    this.executor = executor;
    this.connPool = connPool;
    this.counter = new AtomicLong(1);
  }

  public HttpAsyncRequestConsumer<ProxyHttpExchange> processRequest (final HttpRequest request, final HttpContext context) {

    ProxyHttpExchange httpExchange = (ProxyHttpExchange)context.getAttribute("http-exchange");
    if (httpExchange == null) {
      httpExchange = new ProxyHttpExchange();
      context.setAttribute("http-exchange", httpExchange);
    }
    synchronized (httpExchange) {
      httpExchange.reset();
      String id = String.format("%08X", this.counter.getAndIncrement());
      httpExchange.setId(id);
      httpExchange.setTarget(this.target);
      return new ProxyRequestConsumer(httpExchange, this.executor, this.connPool);
    }
  }

  public void handle (final ProxyHttpExchange httpExchange, final HttpAsyncExchange responseTrigger, final HttpContext context) throws HttpException, IOException {

    synchronized (httpExchange) {
      Exception exception = httpExchange.getException();
      if (exception != null) {
        LoggerManager.getLogger(ProxyRequestHandler.class).error(exception);

        int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_0, status, EnglishReasonPhraseCatalog.INSTANCE.getReason(status, Locale.US));
        String message = exception.getMessage();
        if (message == null) {
          message = "Unexpected error";
        }
        response.setEntity(new NStringEntity(message, ContentType.DEFAULT_TEXT));
        responseTrigger.submitResponse(new BasicAsyncResponseProducer(response));
        LoggerManager.getLogger(ProxyRequestHandler.class).trace("[client<-proxy] %s error response triggered", httpExchange.getId());
      }
      HttpResponse response = httpExchange.getResponse();
      if (response != null) {
        responseTrigger.submitResponse(new ProxyResponseProducer(httpExchange));
        LoggerManager.getLogger(ProxyRequestHandler.class).trace("[client<-proxy] %s response triggered", httpExchange.getId());
      }
      // No response yet.
      httpExchange.setResponseTrigger(responseTrigger);
    }
  }
}