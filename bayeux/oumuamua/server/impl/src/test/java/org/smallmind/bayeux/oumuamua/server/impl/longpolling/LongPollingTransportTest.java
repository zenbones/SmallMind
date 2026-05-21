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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LongPollingTransportTest {

  private ServletProtocol<OrthodoxValue> servletProtocol;
  private LongPollingTransport<OrthodoxValue> transport;

  @BeforeMethod
  public void beforeMethod () {

    servletProtocol = new ServletProtocol<>(30000L);
    transport = (LongPollingTransport<OrthodoxValue>)servletProtocol.getTransport(Transports.LONG_POLLING.getName());
  }

  public void testGetNameReturnsLongPolling () {

    Assert.assertEquals(transport.getName(), Transports.LONG_POLLING.getName());
    Assert.assertEquals(transport.getName(), "long-polling");
  }

  public void testIsLocalReturnsFalse () {

    Assert.assertFalse(transport.isLocal());
  }

  public void testGetProtocolReturnsOwningServletProtocol () {

    Assert.assertSame(transport.getProtocol(), servletProtocol);
  }

  public void testInitIsNoOp () {

    transport.init(null, null);
  }

  public void testAttributeStorageOnTransport () {

    transport.setAttribute("key", "value");

    Assert.assertEquals(transport.getAttribute("key"), "value");
    Assert.assertTrue(transport.getAttributeNames().contains("key"));

    transport.removeAttribute("key");

    Assert.assertNull(transport.getAttribute("key"));
    Assert.assertFalse(transport.getAttributeNames().contains("key"));
  }
}
