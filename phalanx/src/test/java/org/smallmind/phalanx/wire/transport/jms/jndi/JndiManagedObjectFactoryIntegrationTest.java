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
package org.smallmind.phalanx.wire.transport.jms.jndi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.Queue;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.quorum.pool.complex.AbstractComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises {@link JndiManagedObjectFactory} end to end against an in-process Artemis broker whose
 * connection factory and queue are published through Artemis' JNDI provider. The factory is fed a
 * real {@link ComponentPool} of JNDI {@link Context}s, so the lookup, cast, and connection-creation
 * paths run exactly as they would in a JNDI deployment.
 */
@Test(groups = "integration")
public class JndiManagedObjectFactoryIntegrationTest {

  private EmbeddedActiveMQ broker;
  private ComponentPool<Context> contextPool;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    //  The complex pool's Claxon instrumentation reads the per-application context off this thread.
    new PerApplicationContext();

    ConfigurationImpl configuration = new ConfigurationImpl();

    configuration.setPersistenceEnabled(false);
    configuration.setSecurityEnabled(false);
    configuration.addAcceptorConfiguration("in-vm", "vm://0");

    broker = new EmbeddedActiveMQ();
    broker.setConfiguration(configuration);
    broker.start();

    final Hashtable<String, Object> environment = new Hashtable<>();

    environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
    environment.put("connectionFactory.testConnectionFactory", "vm://0");
    environment.put("queue.testQueue", "testQueue");

    contextPool = new ComponentPool<>("jndi-test-pool", new AbstractComponentInstanceFactory<Context>() {

      @Override
      public ComponentInstance<Context> createInstance (ComponentPool<Context> componentPool)
        throws Exception {

        final Context context = new InitialContext(environment);

        return new ComponentInstance<>() {

          @Override
          public boolean validate () {

            return true;
          }

          @Override
          public Context serve () {

            return context;
          }

          @Override
          public void close ()
            throws Exception {

            context.close();
          }

          @Override
          public StackTraceElement[] getExistentialStackTrace () {

            return null;
          }
        };
      }
    }, new ComplexPoolConfig().setInitialPoolSize(4));
    contextPool.startup();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (contextPool != null) {
      contextPool.shutdown();
    }
    if (broker != null) {
      broker.stop();
    }
  }

  @Test
  public void testLookupConnectionFactoryAndDestination ()
    throws Exception {

    JndiManagedObjectFactory factory = new JndiManagedObjectFactory(new JmsConnectionDetails(contextPool, "testQueue", "testConnectionFactory", null, null));

    Connection connection = factory.createConnection();
    try {
      Assert.assertNotNull(connection);
    } finally {
      connection.close();
    }

    Destination destination = factory.getDestination();

    Assert.assertTrue(destination instanceof Queue);
  }
}
