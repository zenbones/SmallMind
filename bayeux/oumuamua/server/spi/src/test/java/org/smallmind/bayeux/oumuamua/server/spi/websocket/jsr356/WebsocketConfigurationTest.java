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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class WebsocketConfigurationTest {

  private static final class StubEndpoint extends Endpoint {

    @Override
    public void onOpen (jakarta.websocket.Session session, jakarta.websocket.EndpointConfig config) {

    }
  }

  public void testConstructorStoresMandatoryFields () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/cometd/*");

    Assert.assertSame(config.getEndpointClass(), StubEndpoint.class);
    Assert.assertEquals(config.getOumuamuaUrl(), "/cometd/*");
  }

  public void testDefaultMaxIdleTimeoutIsNegativeOne () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    Assert.assertEquals(config.getMaxIdleTimeoutMilliseconds(), -1L);
  }

  public void testDefaultAsyncSendTimeoutIsZero () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    Assert.assertEquals(config.getAsyncSendTimeoutMilliseconds(), 0L);
  }

  public void testDefaultMaximumTextMessageBufferSizeIsNegativeOne () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    Assert.assertEquals(config.getMaximumTextMessageBufferSize(), -1);
  }

  public void testDefaultExtensionsIsNull () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    Assert.assertNull(config.getExtensions());
  }

  public void testDefaultSubProtocolIsNull () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    Assert.assertNull(config.getSubProtocol());
  }

  public void testSetMaxIdleTimeoutRoundTrips () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    config.setMaxIdleTimeoutMilliseconds(30000L);

    Assert.assertEquals(config.getMaxIdleTimeoutMilliseconds(), 30000L);
  }

  public void testSetAsyncSendTimeoutRoundTrips () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    config.setAsyncSendTimeoutMilliseconds(5000L);

    Assert.assertEquals(config.getAsyncSendTimeoutMilliseconds(), 5000L);
  }

  public void testSetMaximumTextMessageBufferSizeRoundTrips () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    config.setMaximumTextMessageBufferSize(65536);

    Assert.assertEquals(config.getMaximumTextMessageBufferSize(), 65536);
  }

  public void testSetSubProtocolRoundTrips () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    config.setSubProtocol("bayeux");

    Assert.assertEquals(config.getSubProtocol(), "bayeux");
  }

  public void testSetExtensionsRoundTrips () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");
    Extension ext = new Extension() {

      @Override
      public String getName () {

        return "permessage-deflate";
      }

      @Override
      public java.util.List<Parameter> getParameters () {

        return java.util.Collections.emptyList();
      }
    };

    config.setExtensions(new Extension[] {ext});

    Assert.assertEquals(config.getExtensions().length, 1);
    Assert.assertSame(config.getExtensions()[0], ext);
  }

  public void testSetExtensionsToNullClearsExtensions () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    config.setExtensions(new Extension[0]);
    config.setExtensions(null);

    Assert.assertNull(config.getExtensions());
  }
}
