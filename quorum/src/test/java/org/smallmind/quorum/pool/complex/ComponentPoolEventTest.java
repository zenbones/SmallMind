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
package org.smallmind.quorum.pool.complex;

import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.PoolComponentSupport.InstanceFactory;
import org.smallmind.quorum.pool.complex.PoolComponentSupport.RecordingListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ComponentPoolEventTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation reads a PerApplicationContext, which throws when none is
    // attached to the thread; install an empty one so Instrument resolves to its no-op.
    new PerApplicationContext();
  }

  public void testErrorEventIsBroadcastToRegisteredListeners () {

    ComponentPool<String> pool = new ComponentPool<>("events", new InstanceFactory());
    RecordingListener listener = new RecordingListener();
    Exception cause = new Exception("boom");

    pool.addComponentPoolEventListener(listener);
    pool.reportErrorOccurred(cause);

    Assert.assertEquals(listener.getErrorEvents().size(), 1);
    Assert.assertSame(listener.getErrorEvents().get(0).getException(), cause);
    Assert.assertSame(listener.getErrorEvents().get(0).getSource(), pool);
  }

  public void testRemovedListenerIsNoLongerNotified () {

    ComponentPool<String> pool = new ComponentPool<>("events", new InstanceFactory());
    RecordingListener listener = new RecordingListener();

    pool.addComponentPoolEventListener(listener);
    pool.removeComponentPoolEventListener(listener);
    pool.reportErrorOccurred(new Exception("ignored"));

    Assert.assertEquals(listener.getErrorEvents().size(), 0);
  }

  public void testLeaseTimeEventCarriesTheReportedDuration () {

    ComponentPool<String> pool = new ComponentPool<>("events", new InstanceFactory());
    RecordingListener listener = new RecordingListener();

    pool.addComponentPoolEventListener(listener);
    pool.reportLeaseTimeNanos(4242L);

    Assert.assertEquals(listener.getLeaseEvents().size(), 1);
    Assert.assertEquals(listener.getLeaseEvents().get(0).getLeaseTimeNanos(), 4242L);
  }

  public void testReturningAComponentFiresALeaseTimeEventWhenEnabled ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("events", factory, new ComplexPoolConfig().setInitialPoolSize(1).setReportLeaseTimeNanos(true));
    RecordingListener listener = new RecordingListener();

    pool.addComponentPoolEventListener(listener);
    pool.startup();
    try {
      pool.getComponent();
      pool.returnInstance(factory.instance(0));

      Assert.assertEquals(listener.getLeaseEvents().size(), 1, "returning a component with lease reporting enabled should fire one event");
      Assert.assertTrue(listener.getLeaseEvents().get(0).getLeaseTimeNanos() >= 0);
    } finally {
      pool.shutdown();
    }
  }
}
