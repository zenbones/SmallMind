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
package org.smallmind.bayeux.cometd.transport;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import org.smallmind.bayeux.cometd.logging.DataRecord;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.scribe.pen.LoggerManager;

public class AsyncWindow {

  private final ThreadLocal<AsyncContext> asyncContextThreadLocal = new ThreadLocal<>();
  private final LinkedList<OumuamuaPacket> sentPacketList = new LinkedList<>();
  private final OumuamuaCarrier carrier;

  public AsyncWindow (OumuamuaCarrier carrier) {

    this.carrier = carrier;
  }

  public void addAsyncContext (AsyncContext asyncContext) {

    asyncContextThreadLocal.set(asyncContext);
  }

  public void clearAsyncContext () {

    asyncContextThreadLocal.remove();
  }

  public String getUserAgent () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextThreadLocal.get()) == null) ? null : ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  public HttpServletRequest getRequest () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextThreadLocal.get()) == null) ? null : (HttpServletRequest)asyncContext.getRequest();
  }

  public synchronized void send (OumuamuaPacket... packets)
    throws IOException {

    AsyncContext asyncContext;

    if ((asyncContext = asyncContextThreadLocal.get()) == null) {
      if (packets != null) {
        sentPacketList.addAll(Arrays.asList(packets));
      }
    } else {

      String text;

      if (sentPacketList.isEmpty()) {
        text = carrier.asText(packets);
      } else {

        OumuamuaPacket[] combinedPackets = new OumuamuaPacket[sentPacketList.size() + ((packets == null) ? 0 : packets.length)];
        int index = 0;

        for (OumuamuaPacket sentPacket : sentPacketList) {
          combinedPackets[index++] = sentPacket;
        }

        if (packets != null) {
          System.arraycopy(packets, 0, combinedPackets, index, packets.length);
        }

        text = carrier.asText(combinedPackets);
      }

      if (text != null) {

        System.out.println("=>" + text);
        LoggerManager.getLogger(LongPollingCarrier.class).debug(new DataRecord(text, false));

        asyncContext.getResponse().getOutputStream().print(text);
        asyncContext.getResponse().flushBuffer();
      }
    }
  }
}
