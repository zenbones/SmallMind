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
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HttpContext;
import org.smallmind.scribe.pen.LoggerManager;

public class ProxyResponseProducer implements HttpAsyncResponseProducer {

  private final ProxyHttpExchange httpExchange;

  public ProxyResponseProducer (final ProxyHttpExchange httpExchange) {

    super();
    this.httpExchange = httpExchange;
  }

  public void close () throws IOException {

    this.httpExchange.reset();
  }

  public HttpResponse generateResponse () {

    synchronized (this.httpExchange) {
      HttpResponse response = this.httpExchange.getResponse();
      LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s %s", this.httpExchange.getId(), response.getStatusLine());
      // Rewrite response!!!!
      BasicHttpResponse r = new BasicHttpResponse(response.getStatusLine());
      r.setEntity(response.getEntity());

      return r;
    }
  }

  public void produceContent (final ContentEncoder encoder, final IOControl ioctrl) throws IOException {

    synchronized (this.httpExchange) {
      this.httpExchange.setClientIOControl(ioctrl);
      // Send data to the client
      ByteBuffer buf = this.httpExchange.getOutBuffer();
      buf.flip();
      int n = encoder.write(buf);
      buf.compact();
      LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s %d bytes written", this.httpExchange.getId(), n);
      // If there is space in the buffer and the message has not been
      // transferred, make sure the origin is sending more data
      if (buf.hasRemaining() && !this.httpExchange.isResponseReceived()) {
        if (this.httpExchange.getOriginIOControl() != null) {
          this.httpExchange.getOriginIOControl().requestInput();
          LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s request origin input", this.httpExchange.getId());
        }
      }
      if (buf.position() == 0) {
        if (this.httpExchange.isResponseReceived()) {
          encoder.complete();
          LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s content fully written", this.httpExchange.getId());
        } else {
          // Input buffer is empty. Wait until the origin fills up
          // the buffer
          ioctrl.suspendOutput();
          LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s suspend client output", this.httpExchange.getId());
        }
      }
    }
  }

  public void responseCompleted (final HttpContext context) {

    synchronized (this.httpExchange) {
      LoggerManager.getLogger(ProxyResponseProducer.class).trace("[client<-proxy] %s response completed", this.httpExchange.getId());
    }
  }

  public void failed (final Exception exception) {

    LoggerManager.getLogger(ProxyResponseProducer.class).error(exception);
  }
}
