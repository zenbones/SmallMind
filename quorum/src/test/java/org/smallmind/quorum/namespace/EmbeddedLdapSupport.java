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
package org.smallmind.quorum.namespace;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;

/**
 * Test helper that boots an UnboundID {@link InMemoryDirectoryServer} — a pure-Java, in-process LDAP
 * server requiring no Docker daemon — seeded with a small {@code cn=}-rooted tree. Used by the
 * {@code java:} namespace integration tests to exercise the LDAP backing store end-to-end.
 * <p>
 * Schema enforcement is disabled so the JNDI provider's bare {@code createSubcontext}/modify calls,
 * which do not always carry a complete object-class set, are accepted as-is.
 */
public final class EmbeddedLdapSupport {

  public static final String BASE_DN = "dc=smallmind,dc=org";
  public static final String BIND_DN = "cn=Directory Manager";
  public static final String PASSWORD = "password";

  private EmbeddedLdapSupport () {

  }

  /**
   * Starts a freshly seeded in-memory LDAP server on an ephemeral port and returns it. Callers must
   * {@link InMemoryDirectoryServer#shutDown(boolean)} the result when done.
   *
   * @return a started in-memory directory server holding {@code cn=alpha} and {@code cn=beta}
   * @throws Exception if the server cannot be configured, populated, or started
   */
  public static InMemoryDirectoryServer start ()
    throws Exception {

    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(BASE_DN);

    config.addAdditionalBindCredentials(BIND_DN, PASSWORD);
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 0));
    config.setSchema(null);

    InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);

    server.add("dn: " + BASE_DN, "objectClass: top", "objectClass: domain", "dc: smallmind");
    server.add("dn: cn=alpha," + BASE_DN, "objectClass: top", "objectClass: applicationProcess", "cn: alpha");
    server.add("dn: cn=beta," + BASE_DN, "objectClass: top", "objectClass: applicationProcess", "cn: beta");
    server.startListening();

    return server;
  }
}
