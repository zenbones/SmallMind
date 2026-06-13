/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.web.grizzly;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.smallmind.web.grizzly.option.WebSocketOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration scenario that boots a Grizzly server with a {@link WebSocketOption} enabled and WSADL support turned on,
 * lifting the WebSocket container creation, start, and WSADL handler branches of {@link GrizzlyInitializingBean} and
 * {@link org.smallmind.web.grizzly.tyrus.TyrusGrizzlyServerContainer}. The WSADL descriptor is then fetched over HTTP
 * to exercise {@link org.smallmind.web.grizzly.tyrus.WsadlHttpHandler}.
 */
@Test(groups = "integration")
public class GrizzlyWebSocketBootTest {

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  public void testWebSocketOptionWithWsadlBootsAndServesDescriptor ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);

        WebSocketOption webSocketOption = new WebSocketOption();
        webSocketOption.setIncludeWsadlSupport(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/ws");
        webApplicationOption.setWebSocketOption(webSocketOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/application.wsadl")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertTrue(response.body().contains("application"), "WSADL document should be returned");
    }
  }
}
