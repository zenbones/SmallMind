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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.HashMap;
import java.util.Map;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import org.cometd.client.BayeuxClient;
import org.cometd.client.ext.AckExtension;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.websocket.javax.WebSocketTransport;
import org.smallmind.nutsnbolts.util.Counter;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

@Test
public class AckExtTest {

  /*
  <=[{"supportedConnectionTypes":["websocket"],"channel":"/meta/handshake","id":"1","version":"1.0"}]
=>[{"minimumVersion":"1.0","clientId":"0000018abf6fef2aca57d86a288a15548002","supportedConnectionTypes":["websocket"],"channel":"/meta/handshake","id":"1","version":"1.0","successful":true}]
<=[{"clientId":"0000018abf6fef2aca57d86a288a15548002","advice":{"timeout":0},"channel":"/meta/connect","id":"2","connectionType":"websocket"}]
=>[{"clientId":"0000018abf6fef2aca57d86a288a15548002","advice":{"interval":30000},"channel":"/meta/connect","id":"2","successful":true}]
<=[{"clientId":"0000018abf6fef2aca57d86a288a15548002","channel":"/meta/subscribe","id":"3","subscription":"/foobar"}]
=>[{"clientId":"0000018abf6fef2aca57d86a288a15548002","channel":"/meta/subscribe","id":"3","subscription":"/foobar","successful":true}]

   */

  public void test ()
    throws Exception {

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/smallmind/bayeux/oumuamua/server/impl/logging.xml", "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml", "org/smallmind/bayeux/oumuamua/server/impl/oumuamua.xml");
    ClientTransport wsTransport;
    WebSocketContainer webSocketContainer;
    BayeuxClient bayeuxClient;

    webSocketContainer = ContainerProvider.getWebSocketContainer();
    wsTransport = new WebSocketTransport(null, null, webSocketContainer);

    bayeuxClient = new BayeuxClient("http://localhost:9017/smallmind/cometd", wsTransport);
    bayeuxClient.addExtension(new AckExtension());

    Map<String, Object> handshakeMap = new HashMap<>();
    HashMap<String, Object> tokenMap = new HashMap<>();

    // handshakeMap.put("ext", tokenMap);

    bayeuxClient.handshake(handshakeMap, System.out::println);
    if (!bayeuxClient.waitFor(5000, BayeuxClient.State.CONNECTED)) {
      System.out.println("Unable to connect within 5000 milliseconds");
    }

    Counter counter = new Counter();

    bayeuxClient.getChannel("/foobar").subscribe((channel, message) -> {
      System.out.println("!!!!!!!!!!!!!!!:" + message);
      if (counter.incAndGet() == 10) {
        System.out.println(System.currentTimeMillis());
      }
    });

    Thread.sleep(300000);
    System.out.println("Done...");
  }
}
