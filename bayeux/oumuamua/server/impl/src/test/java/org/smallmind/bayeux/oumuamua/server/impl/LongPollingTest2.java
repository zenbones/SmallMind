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
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.testng.annotations.Test;

@Test
public class LongPollingTest2 {

  /*
  [{"minimumVersion":"1.0","clientId":"11djxhxdr697ckmu8pk0vogwct",
  "supportedConnectionTypes":["websocket","long-polling","callback-polling"],
  "advice":{"interval":0,"timeout":30000,"reconnect":"retry"},
  "channel":"/meta/handshake","id":"1","version":"1.0","successful":true}]
  [{"advice":{"interval":0,"timeout":30000,"reconnect":"retry"},"channel":"/meta/connect","id":"2","successful":true}]
   */

  public void test ()
    throws Exception {

    // ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/smallmind/bayeux/oumuamua/oumuamua-grizzly.xml", "org/smallmind/bayeux/oumuamua/oumuamua.xml");
    HttpClient httpClient = new HttpClient();
    httpClient.start();
    JettyHttpClientTransport transport = new JettyHttpClientTransport(new HashMap<>(), httpClient);
    BayeuxClient bayeuxClient;

    bayeuxClient = new BayeuxClient("http://localhost:9017/smallmind/cometd", transport);

    Map<String, Object> handshakeMap = new HashMap<>();
    HashMap<String, Object> tokenMap = new HashMap<>();

    // handshakeMap.put("ext", tokenMap);

    bayeuxClient.handshake(handshakeMap);
    if (!bayeuxClient.waitFor(500, BayeuxClient.State.CONNECTED)) {
      System.out.println("Unable to connect within 5000 milliseconds");
    }

    ClientSessionChannel channel = bayeuxClient.getChannel("/foobar");

    System.out.println(System.currentTimeMillis());
    for (int i = 0; i < 100; i++) {
      channel.publish("{\"x\":1, \"y\":2}", message -> message.toString());
      Thread.sleep(33);
    }

    System.out.println("Done...");
    Thread.sleep(300000);
  }
}
