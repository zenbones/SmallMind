/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.cometd.oumuamua.transport;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.context.OumuamuaServletContext;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;

public class LongPollingCarrier implements OumuamuaCarrier {

  private static final String[] ACTUAL_TRANSPORTS = new String[] {"long-polling"};
  private final OumuamuaServletContext context;
  private final LongPollingTransport longPollingTransport;
  private final AsyncContext asyncContext;
  private boolean connected;

  public LongPollingCarrier (LongPollingTransport longPollingTransport, AsyncContext asyncContext)
    throws IOException {

    this.longPollingTransport = longPollingTransport;
    this.asyncContext = asyncContext;

    context = new OumuamuaServletContext((HttpServletRequest)asyncContext.getRequest());
    asyncContext.getResponse().getOutputStream().setWriteListener();
  }

  @Override
  public String[] getActualTransports () {

    return ACTUAL_TRANSPORTS;
  }

  @Override
  public BayeuxContext getContext () {

    return context;
  }

  @Override
  public String getUserAgent () {

    return ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : longPollingTransport.getMaxInterval();

    asyncContext.setTimeout(Math.max(adjustedIdleTimeout, 0));
  }

  @Override
  public synchronized boolean isConnected (String sessionId) {

    return connected;
  }

  @Override
  public synchronized void setConnected (String sessionId, boolean connected) {

    this.connected = connected;
  }

  @Override
  public synchronized void send (OumuamuaPacket... packets)
    throws Exception {

  }

  @Override
  public synchronized OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    return new OumuamuaPacket[0];
  }

  @Override
  public synchronized void close ()
    throws IOException {

  }
}
