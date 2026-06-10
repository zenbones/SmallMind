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
package org.smallmind.quorum.namespace.backingStore.ldap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import org.smallmind.quorum.namespace.EmbeddedLdapSupport;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises {@link LdapContextCreator} against an embedded, in-process LDAP server: the non-TLS
 * connection path, the {@code insureContext} intermediate-node creation (both the lookup-hits and the
 * not-found-then-create arms), the root accessor, and the TLS branch of the environment builder.
 */
@Test(groups = "integration")
public class LdapContextCreatorIntegrationTest {

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

  private LdapContextCreator creator (boolean tls) {

    return new LdapContextCreator(new NamingConnectionDetails("localhost", port, tls, EmbeddedLdapSupport.BASE_DN, EmbeddedLdapSupport.BIND_DN, EmbeddedLdapSupport.PASSWORD));
  }

  public void testGetRootReturnsTheConfiguredBaseDn () {

    Assert.assertEquals(creator(false).getRoot(), EmbeddedLdapSupport.BASE_DN, "the root should be the configured base DN");
  }

  public void testGetInitialContextConnectsAndResolvesASeededEntry ()
    throws NamingException {

    DirContext context = creator(false).getInitialContext();

    try {
      Assert.assertNotNull(context.lookup("cn=alpha"), "a live context rooted at the base DN should resolve a seeded entry");
    } finally {
      context.close();
    }
  }

  public void testInsureContextCreatesMissingNodesAndIsIdempotent ()
    throws NamingException {

    DirContext context = creator(false).getInitialContext();

    try {
      // cn=alpha already exists (lookup arm); cn=child,cn=alpha does not (not-found-then-create arm).
      LdapContextCreator.insureContext(context, "cn=child,cn=alpha");

      Assert.assertNotNull(context.lookup("cn=child,cn=alpha"), "the missing intermediate node should have been created");

      // A second pass finds every node present, so it takes only the lookup arm and creates nothing new.
      LdapContextCreator.insureContext(context, "cn=child,cn=alpha");
      Assert.assertNotNull(context.lookup("cn=child,cn=alpha"), "insureContext should be idempotent over an existing path");
    } finally {
      context.close();
    }
  }

  public void testTlsConnectionAttemptUsesTheLdapsBranch () {

    // The embedded server speaks plaintext, so an ldaps attempt cannot complete — but reaching the
    // failure first executes the ldaps URL construction and the SECURITY_PROTOCOL=ssl branch.
    Assert.assertThrows(NamingException.class, () -> creator(true).getInitialContext());
  }
}
