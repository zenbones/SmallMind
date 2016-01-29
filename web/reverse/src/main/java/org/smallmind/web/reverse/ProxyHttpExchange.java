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

import java.nio.ByteBuffer;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncExchange;

public class ProxyHttpExchange {

  private final ByteBuffer inBuffer;
  private final ByteBuffer outBuffer;

  private volatile String id;
  private volatile HttpHost target;
  private volatile HttpAsyncExchange responseTrigger;
  private volatile IOControl originIOControl;
  private volatile IOControl clientIOControl;
  private volatile HttpRequest request;
  private volatile boolean requestReceived;
  private volatile HttpResponse response;
  private volatile boolean responseReceived;
  private volatile Exception ex;

  public ProxyHttpExchange () {

    super();
    this.inBuffer = ByteBuffer.allocateDirect(10240);
    this.outBuffer = ByteBuffer.allocateDirect(10240);
  }

  public ByteBuffer getInBuffer () {

    return this.inBuffer;
  }

  public ByteBuffer getOutBuffer () {

    return this.outBuffer;
  }

  public String getId () {

    return this.id;
  }

  public void setId (final String id) {

    this.id = id;
  }

  public HttpHost getTarget () {

    return this.target;
  }

  public void setTarget (final HttpHost target) {

    this.target = target;
  }

  public HttpRequest getRequest () {

    return this.request;
  }

  public void setRequest (final HttpRequest request) {

    this.request = request;
  }

  public HttpResponse getResponse () {

    return this.response;
  }

  public void setResponse (final HttpResponse response) {

    this.response = response;
  }

  public HttpAsyncExchange getResponseTrigger () {

    return this.responseTrigger;
  }

  public void setResponseTrigger (final HttpAsyncExchange responseTrigger) {

    this.responseTrigger = responseTrigger;
  }

  public IOControl getClientIOControl () {

    return this.clientIOControl;
  }

  public void setClientIOControl (final IOControl clientIOControl) {

    this.clientIOControl = clientIOControl;
  }

  public IOControl getOriginIOControl () {

    return this.originIOControl;
  }

  public void setOriginIOControl (final IOControl originIOControl) {

    this.originIOControl = originIOControl;
  }

  public boolean isRequestReceived () {

    return this.requestReceived;
  }

  public void setRequestReceived () {

    this.requestReceived = true;
  }

  public boolean isResponseReceived () {

    return this.responseReceived;
  }

  public void setResponseReceived () {

    this.responseReceived = true;
  }

  public Exception getException () {

    return this.ex;
  }

  public void setException (final Exception ex) {

    this.ex = ex;
  }

  public void reset () {

    this.inBuffer.clear();
    this.outBuffer.clear();
    this.target = null;
    this.id = null;
    this.responseTrigger = null;
    this.clientIOControl = null;
    this.originIOControl = null;
    this.request = null;
    this.requestReceived = false;
    this.response = null;
    this.responseReceived = false;
    this.ex = null;
  }
}
