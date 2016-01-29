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
import java.nio.ByteBuffer;
import org.apache.http.HttpRequest;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.protocol.HttpContext;
import org.smallmind.scribe.pen.LoggerManager;

public class ProxyRequestConsumer implements HttpAsyncRequestConsumer<ProxyHttpExchange> {

  private final ProxyHttpExchange httpExchange;
  private final HttpAsyncRequester executor;
  private final BasicNIOConnPool connPool;

  private volatile boolean completed;

  public ProxyRequestConsumer (final ProxyHttpExchange httpExchange, final HttpAsyncRequester executor, final BasicNIOConnPool connPool) {

    super();
    this.httpExchange = httpExchange;
    this.executor = executor;
    this.connPool = connPool;
  }

  public void close () throws IOException {

  }

  public void requestReceived (final HttpRequest request) {

    synchronized (this.httpExchange) {
      LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s %s", this.httpExchange.getId(), request.getRequestLine());

      this.httpExchange.setRequest(request);
      this.executor.execute(new ProxyRequestProducer(this.httpExchange), new ProxyResponseConsumer(this.httpExchange), this.connPool);
    }
  }

  public void consumeContent (final ContentDecoder decoder, final IOControl ioctrl) throws IOException {

    synchronized (this.httpExchange) {
      this.httpExchange.setClientIOControl(ioctrl);
      // Receive data from the client
      ByteBuffer buf = this.httpExchange.getInBuffer();
      int n = decoder.read(buf);
      LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s %d bytes read", this.httpExchange.getId(), n);
      if (decoder.isCompleted()) {
        LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s content fully read", this.httpExchange.getId());
      }
      // If the buffer is full, suspend client input until there is free
      // space in the buffer
      if (!buf.hasRemaining()) {
        ioctrl.suspendInput();
        LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s suspend client input", this.httpExchange.getId());
      }
      // If there is some content in the input buffer make sure origin
      // output is active
      if (buf.position() > 0) {
        if (this.httpExchange.getOriginIOControl() != null) {
          this.httpExchange.getOriginIOControl().requestOutput();
          LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s request origin output", this.httpExchange.getId());
        }
      }
    }
  }

  public void requestCompleted (final HttpContext context) {

    synchronized (this.httpExchange) {
      this.completed = true;

      LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s request completed", this.httpExchange.getId());

      this.httpExchange.setRequestReceived();
      if (this.httpExchange.getOriginIOControl() != null) {
        this.httpExchange.getOriginIOControl().requestOutput();
        LoggerManager.getLogger(ProxyRequestConsumer.class).trace("[client->proxy] %s request origin output", this.httpExchange.getId());
      }
    }
  }

  public Exception getException () {

    return null;
  }

  public ProxyHttpExchange getResult () {

    return this.httpExchange;
  }

  public boolean isDone () {

    return this.completed;
  }

  public void failed (final Exception exception) {

    LoggerManager.getLogger(ProxyRequestConsumer.class).error(exception);
  }
}