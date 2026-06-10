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
package org.smallmind.phalanx.wire.jmx;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the {@link ResponseTransportMonitor} JMX adapter both as a direct delegate to its
 * {@link ResponseTransport} and as a registered {@link javax.management.StandardMBean}, reading the
 * {@code State} attribute and invoking {@code pause}/{@code play} through a real {@link MBeanServer}.
 */
@Test(groups = "unit")
public class ResponseTransportMonitorTest {

  @Test
  public void testDirectDelegation ()
    throws Exception {

    StubResponseTransport transport = new StubResponseTransport();
    ResponseTransportMonitor monitor = new ResponseTransportMonitor(transport);

    Assert.assertEquals(monitor.getState(), TransportState.PLAYING);

    monitor.pause();
    Assert.assertEquals(transport.getState(), TransportState.PAUSED);

    monitor.play();
    Assert.assertEquals(transport.getState(), TransportState.PLAYING);
  }

  @Test
  public void testManagedThroughMBeanServer ()
    throws Exception {

    StubResponseTransport transport = new StubResponseTransport();
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName("org.smallmind.phalanx.test:type=ResponseTransport,id=monitor");

    mBeanServer.registerMBean(new ResponseTransportMonitor(transport), objectName);
    try {
      //  ResponseTransportMonitor is exposed as an MXBean, so JMX maps the TransportState enum to its String name.
      Assert.assertEquals(mBeanServer.getAttribute(objectName, "State"), TransportState.PLAYING.name());

      mBeanServer.invoke(objectName, "pause", new Object[0], new String[0]);
      Assert.assertEquals(mBeanServer.getAttribute(objectName, "State"), TransportState.PAUSED.name());

      mBeanServer.invoke(objectName, "play", new Object[0], new String[0]);
      Assert.assertEquals(mBeanServer.getAttribute(objectName, "State"), TransportState.PLAYING.name());
    } finally {
      mBeanServer.unregisterMBean(objectName);
    }
  }

  private static class StubResponseTransport implements ResponseTransport {

    private TransportState state = TransportState.PLAYING;

    @Override
    public String getInstanceId () {

      return "stub";
    }

    @Override
    public String register (Class<?> serviceInterface, WiredService targetService) {

      return "stub";
    }

    @Override
    public TransportState getState () {

      return state;
    }

    @Override
    public void play () {

      state = TransportState.PLAYING;
    }

    @Override
    public void pause () {

      state = TransportState.PAUSED;
    }

    @Override
    public void close () {

      state = TransportState.CLOSED;
    }
  }
}
