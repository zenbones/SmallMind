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
package org.smallmind.quorum.namespace.pool;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.quorum.namespace.EmbeddedLdapSupport;
import org.smallmind.quorum.namespace.PooledJavaContext;
import org.smallmind.quorum.namespace.backingStore.StorageType;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Drives the full namespace pooling stack — {@link PooledJavaContextComponentInstanceFactory} minting
 * {@link JavaContextComponentInstance}s under a real {@link ComponentPool} — against an embedded LDAP
 * server. A borrowed context is validated on acquire, served, logically closed (which returns it to
 * the pool), reused, and finally retired on shutdown.
 * <p>
 * The factory resolves its {@link PooledJavaContext} through {@code new InitialContext(env)} using
 * {@link org.smallmind.quorum.namespace.JavaURLContextFactory} as the initial-context factory.
 */
@Test(groups = "integration")
public class PooledJavaContextIntegrationTest {

  private InMemoryDirectoryServer server;
  private int port;

  @BeforeClass
  public void startServer ()
    throws Exception {

    server = EmbeddedLdapSupport.start();
    port = server.getListenPort();
  }

  @AfterClass
  public void stopServer () {

    if (server != null) {
      server.shutDown(true);
    }
  }

  @BeforeMethod
  public void establishPerApplicationContext () {

    new PerApplicationContext();
  }

  private PooledJavaContextComponentInstanceFactory factory () {

    // contextPath "java:" yields a pooled context rooted at the base DN; entries are then addressed
    // by their internal names (e.g. "alpha", which the LDAP translator renders as cn=alpha).
    return new PooledJavaContextComponentInstanceFactory(StorageType.LDAP, "java:", "localhost", port, false, EmbeddedLdapSupport.BASE_DN, EmbeddedLdapSupport.BIND_DN, EmbeddedLdapSupport.PASSWORD);
  }

  public void testValidateBorrowReturnReuseAndRetire ()
    throws Exception {

    // testOnAcquire forces a validate() call (a no-op lookup) against the live server before serving.
    ComponentPool<PooledJavaContext> pool = new ComponentPool<>("ldap-namespace", factory(), new ComplexPoolConfig().setInitialPoolSize(1).setMaxPoolSize(1).setTestOnAcquire(true));

    pool.startup();
    try {
      Assert.assertEquals(pool.getPoolSize(), 1, "the pool should pre-warm a single pooled context");

      PooledJavaContext context = pool.getComponent();

      Assert.assertNotNull(context, "a validated, live context should be served");
      Assert.assertEquals(pool.getProcessingSize(), 1);
      Assert.assertEquals(pool.getFreeSize(), 0);

      // A logical close fires a context-closed event; the component instance returns itself to the pool.
      context.close();
      Assert.assertEquals(pool.getFreeSize(), 1, "a logically closed context should be returned to the pool");
      Assert.assertEquals(pool.getProcessingSize(), 0);

      // The single capped instance must be re-servable after its return, proving it was not discarded.
      PooledJavaContext reused = pool.getComponent();
      Assert.assertNotNull(reused);
      Assert.assertSame(reused, context, "the capped pool should re-serve the very same returned context");
      reused.close();
    } finally {
      pool.shutdown();
    }

    Assert.assertEquals(pool.getPoolSize(), 0, "shutdown should retire (physically close) every pooled context");
  }

  public void testServedContextPerformsRealDirectoryOperations ()
    throws Exception {

    ComponentPool<PooledJavaContext> pool = new ComponentPool<>("ldap-namespace-ops", factory(), new ComplexPoolConfig().setInitialPoolSize(1).setMaxPoolSize(1));

    pool.startup();
    try {
      PooledJavaContext context = pool.getComponent();

      // The borrowed, pooled context is a fully functional JavaContext over the live backing store;
      // "alpha" is the internal name that the LDAP translator renders as cn=alpha.
      Assert.assertNotNull(context.getAttributes("alpha").get("cn"), "a borrowed pooled context should resolve real entries");
      Assert.assertEquals(context.getAttributes("alpha").get("cn").get(), "alpha");

      context.close();
    } finally {
      pool.shutdown();
    }
  }
}
