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
package org.smallmind.quorum.pool.complex.jmx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.quorum.pool.complex.AbstractComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies that {@link ComponentPoolMonitor} faithfully bridges a {@link ComponentPool} to JMX:
 * configuration getters and setters delegate to the pool's {@link ComplexPoolConfig}, MBean
 * registration toggles the monitor's pool-event subscription, and pool events are translated into
 * the corresponding typed JMX notifications.
 */
@Test(groups = "unit")
public class ComponentPoolMonitorTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation reads a PerApplicationContext; install an empty one so
    // Instrument resolves to its no-op.
    new PerApplicationContext();
  }

  private ComponentPool<String> pool (ComplexPoolConfig config) {

    return new ComponentPool<>("monitored", new NoOpInstanceFactory(), config);
  }

  public void testConfigurationGettersReflectThePoolConfig () {

    ComplexPoolConfig config = new ComplexPoolConfig().setMaxPoolSize(11).setMinPoolSize(3).setTestOnAcquire(true).setReportLeaseTimeNanos(true).setMaxLeaseTimeSeconds(17).setMaxIdleTimeSeconds(19).setMaxProcessingTimeSeconds(23).setCreationTimeoutMillis(101L).setAcquireWaitTimeMillis(202L);
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool(config));

    Assert.assertEquals(monitor.getPoolName(), "monitored");
    Assert.assertEquals(monitor.getMaxPoolSize(), 11);
    Assert.assertEquals(monitor.getMinPoolSize(), 3);
    Assert.assertTrue(monitor.isTestOnAcquire());
    Assert.assertTrue(monitor.isReportLeaseTimeNanos());
    Assert.assertEquals(monitor.getMaxLeaseTimeSeconds(), 17);
    Assert.assertEquals(monitor.getMaxIdleTimeSeconds(), 19);
    Assert.assertEquals(monitor.getMaxProcessingTimeSeconds(), 23);
    Assert.assertEquals(monitor.getCreationTimeoutMillis(), 101L);
    Assert.assertEquals(monitor.getAcquireWaitTimeMillis(), 202L);
  }

  public void testConfigurationSettersWriteThroughToThePoolConfig () {

    ComplexPoolConfig config = new ComplexPoolConfig();
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool(config));

    monitor.setMaxPoolSize(31);
    monitor.setMinPoolSize(7);
    monitor.setTestOnCreate(true);
    monitor.setExistentiallyAware(true);
    monitor.setMaxLeaseTimeSeconds(41);
    monitor.setAcquireWaitTimeMillis(303L);

    Assert.assertEquals(config.getMaxPoolSize(), 31);
    Assert.assertEquals(config.getMinPoolSize(), 7);
    Assert.assertTrue(config.isTestOnCreate());
    Assert.assertTrue(config.isExistentiallyAware());
    Assert.assertEquals(config.getMaxLeaseTimeSeconds(), 41);
    Assert.assertEquals(config.getAcquireWaitTimeMillis(), 303L);
  }

  public void testSizeAccessorsDelegateToThePool () {

    ComponentPool<String> pool = pool(new ComplexPoolConfig());
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);

    Assert.assertEquals(monitor.getPoolSize(), pool.getPoolSize());
    Assert.assertEquals(monitor.getFreeSize(), pool.getFreeSize());
    Assert.assertEquals(monitor.getProcessingSize(), pool.getProcessingSize());
  }

  public void testRegistrationSubscribesAndErrorEventBecomesANotification ()
    throws Exception {

    ComponentPool<String> pool = pool(new ComplexPoolConfig());
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);
    CapturingListener listener = new CapturingListener();
    ObjectName objectName = ObjectName.getInstance("org.smallmind.quorum.test:type=pool");

    Assert.assertSame(monitor.preRegister(null, objectName), objectName, "preRegister should return the proposed name unchanged");
    monitor.addNotificationListener(listener, null, null);

    Exception cause = new Exception("creation failure");
    pool.reportErrorOccurred(cause);

    Assert.assertEquals(listener.getNotifications().size(), 1);
    Assert.assertTrue(listener.getNotifications().get(0) instanceof CreationErrorOccurredNotification);
    Assert.assertSame(((CreationErrorOccurredNotification)listener.getNotifications().get(0)).getException(), cause);
  }

  public void testLeaseTimeEventBecomesALeaseNotification ()
    throws Exception {

    ComponentPool<String> pool = pool(new ComplexPoolConfig());
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);
    CapturingListener listener = new CapturingListener();

    monitor.preRegister(null, ObjectName.getInstance("org.smallmind.quorum.test:type=pool"));
    monitor.addNotificationListener(listener, null, null);

    pool.reportLeaseTimeNanos(8675L);

    Assert.assertEquals(listener.getNotifications().size(), 1);
    Assert.assertTrue(listener.getNotifications().get(0) instanceof ComponentLeaseTimeNotification);
    Assert.assertEquals(((ComponentLeaseTimeNotification)listener.getNotifications().get(0)).getLeaseTimeNanos(), 8675L);
  }

  public void testDeregistrationUnsubscribesFromPoolEvents ()
    throws Exception {

    ComponentPool<String> pool = pool(new ComplexPoolConfig());
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);
    CapturingListener listener = new CapturingListener();

    monitor.preRegister(null, ObjectName.getInstance("org.smallmind.quorum.test:type=pool"));
    monitor.addNotificationListener(listener, null, null);
    monitor.preDeregister();

    pool.reportErrorOccurred(new Exception("ignored after deregister"));

    Assert.assertTrue(listener.getNotifications().isEmpty(), "a deregistered monitor should no longer translate pool events");
  }

  public void testRemainingConfigurationGettersReflectThePoolConfig () {

    ComplexPoolConfig config = new ComplexPoolConfig().setInitialPoolSize(5).setTestOnCreate(true).setExistentiallyAware(true).setMaxIdleTimeSeconds(13).setMaxProcessingTimeSeconds(29);
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool(config));

    Assert.assertEquals(monitor.getInitialPoolSize(), 5);
    Assert.assertTrue(monitor.isTestOnCreate());
    Assert.assertTrue(monitor.isExistentiallyAware());
    Assert.assertEquals(monitor.getMaxIdleTimeSeconds(), 13);
    Assert.assertEquals(monitor.getMaxProcessingTimeSeconds(), 29);
  }

  public void testRemainingConfigurationSettersWriteThroughToThePoolConfig () {

    ComplexPoolConfig config = new ComplexPoolConfig();
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool(config));

    monitor.setTestOnAcquire(true);
    monitor.setReportLeaseTimeNanos(true);
    monitor.setCreationTimeoutMillis(404L);
    monitor.setMaxIdleTimeSeconds(53);
    monitor.setMaxProcessingTimeSeconds(59);

    Assert.assertTrue(config.isTestOnAcquire());
    Assert.assertTrue(config.isReportLeaseTimeNanos());
    Assert.assertEquals(config.getCreationTimeoutMillis(), 404L);
    Assert.assertEquals(config.getMaxIdleTimeSeconds(), 53);
    Assert.assertEquals(config.getMaxProcessingTimeSeconds(), 59);
  }

  public void testLifecycleOperationsDelegateToThePool ()
    throws Exception {

    ComponentPool<String> pool = pool(new ComplexPoolConfig().setInitialPoolSize(2));
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);

    monitor.startup();
    try {
      Assert.assertEquals(monitor.getPoolSize(), 2, "startup through the monitor should pre-warm the underlying pool");
    } finally {
      monitor.shutdown();
    }

    Assert.assertEquals(monitor.getPoolSize(), 0, "shutdown through the monitor should retire the underlying pool");
  }

  public void testPostRegistrationCallbacksAreSilentNoOps ()
    throws Exception {

    // The two post-* MBeanRegistration hooks carry no behaviour; invoking them must not throw and must
    // not disturb the established subscription.
    ComponentPool<String> pool = pool(new ComplexPoolConfig());
    ComponentPoolMonitor monitor = new ComponentPoolMonitor(pool);
    CapturingListener listener = new CapturingListener();

    monitor.preRegister(null, ObjectName.getInstance("org.smallmind.quorum.test:type=pool"));
    monitor.addNotificationListener(listener, null, null);
    monitor.postRegister(Boolean.TRUE);

    pool.reportErrorOccurred(new Exception("after postRegister"));
    Assert.assertEquals(listener.getNotifications().size(), 1, "postRegister should leave the subscription intact");

    monitor.preDeregister();
    monitor.postDeregister();
  }

  private static class CapturingListener implements NotificationListener {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    private List<Notification> getNotifications () {

      return notifications;
    }

    @Override
    public void handleNotification (Notification notification, Object handback) {

      notifications.add(notification);
    }
  }

  private static class NoOpInstanceFactory extends AbstractComponentInstanceFactory<String> {

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      return new ComponentInstance<>() {

        @Override
        public boolean validate () {

          return true;
        }

        @Override
        public String serve () {

          return "component";
        }

        @Override
        public void close () {

        }

        @Override
        public StackTraceElement[] getExistentialStackTrace () {

          return null;
        }
      };
    }
  }
}
