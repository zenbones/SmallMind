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
package org.smallmind.bayeux.oumuamua;

import java.util.HashMap;
import java.util.Map;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.websocket.javax.WebSocketTransport;
import org.testng.annotations.Test;

@Test
public class OtherTest2 {

  public void test ()
    throws Exception {

    ClientTransport wsTransport;
    WebSocketContainer webSocketContainer;

    BayeuxClient bayeuxClient2;

    webSocketContainer = ContainerProvider.getWebSocketContainer();
    wsTransport = new WebSocketTransport(null, null, webSocketContainer);

    bayeuxClient2 = new BayeuxClient("http://localhost:9017/smallmind/cometd", wsTransport);

    Map<String, Object> handshakeMap2 = new HashMap<>();

    bayeuxClient2.handshake(handshakeMap2);
    if (!bayeuxClient2.waitFor(5000, BayeuxClient.State.CONNECTED)) {
      System.out.println("Unable to connect within 5000 milliseconds");
    }

    bayeuxClient2.getChannel("/foobar").publish(JsonNodeFactory.instance.textNode("hello"));

    Thread.sleep(300000);
    System.out.println("Done...");
  }
}
