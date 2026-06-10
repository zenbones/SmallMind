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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * End-to-end exercise of the {@code java:} namespace against a real (in-process) LDAP backing store.
 * The {@link JavaURLContextFactory} reflectively wires the LDAP {@link org.smallmind.quorum.namespace.backingStore.ContextCreator}
 * and {@link org.smallmind.quorum.namespace.backingStore.NameTranslator}; every operation opens a
 * live connection to the embedded server, so the translation, enumeration-wrapping, and mutation
 * paths are all driven against genuine LDAP responses.
 */
@Test(groups = "integration")
public class JavaContextIntegrationTest {

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

  private JavaContext javaContext (boolean modifiable)
    throws Exception {

    Hashtable<String, Object> environment = new Hashtable<>();

    environment.put(JavaContext.CONTEXT_STORE, "ldap");
    environment.put(JavaContext.CONNECTION_DETAILS, new NamingConnectionDetails("localhost", port, false, EmbeddedLdapSupport.BASE_DN, EmbeddedLdapSupport.BIND_DN, EmbeddedLdapSupport.PASSWORD));
    environment.put(JavaContext.CONTEXT_MODIFIABLE, modifiable ? "true" : "false");

    return (JavaContext)new JavaURLContextFactory().getObjectInstance(null, null, null, environment);
  }

  private Set<String> childNames ()
    throws Exception {

    Set<String> names = new HashSet<>();
    NamingEnumeration<NameClassPair> enumeration = javaContext(false).list("java:");

    while (enumeration.hasMore()) {
      names.add(enumeration.next().getName());
    }
    enumeration.close();

    return names;
  }

  public void testListTranslatesEveryChildNameBackToInternalForm ()
    throws Exception {

    Set<String> names = childNames();

    Assert.assertTrue(names.contains("alpha"), "the seeded cn=alpha entry should surface as the internal name 'alpha'");
    Assert.assertTrue(names.contains("beta"), "the seeded cn=beta entry should surface as the internal name 'beta'");
  }

  public void testSearchReturnsTheMatchingEntriesInTranslatedForm ()
    throws Exception {

    Set<String> names = new HashSet<>();
    NamingEnumeration<SearchResult> enumeration = javaContext(false).search("java:", "(objectClass=applicationProcess)", new SearchControls());

    while (enumeration.hasMore()) {
      names.add(enumeration.next().getName());
    }
    enumeration.close();

    Assert.assertTrue(names.contains("alpha"));
    Assert.assertTrue(names.contains("beta"));
  }

  public void testLookupResolvesASeededEntry ()
    throws Exception {

    Assert.assertNotNull(javaContext(false).lookup("java:alpha"), "a seeded entry should resolve through the java: namespace");
  }

  public void testGetAttributesReturnsTheEntryAttributes ()
    throws Exception {

    Attributes attributes = javaContext(false).getAttributes("java:alpha");

    Assert.assertNotNull(attributes.get("cn"));
    Assert.assertEquals(attributes.get("cn").get(), "alpha");
  }

  public void testCreateAndDestroySubcontextRoundTrip ()
    throws Exception {

    JavaContext context = javaContext(true);
    BasicAttributes attributes = new BasicAttributes();
    BasicAttribute objectClass = new BasicAttribute("objectClass");

    objectClass.add("top");
    objectClass.add("applicationProcess");
    attributes.put(objectClass);
    attributes.put("cn", "gamma");

    context.createSubcontext("java:gamma", attributes);
    Assert.assertTrue(childNames().contains("gamma"), "a created subcontext should appear in a fresh listing");

    context.destroySubcontext("java:gamma");
    Assert.assertFalse(childNames().contains("gamma"), "a destroyed subcontext should no longer appear");
  }

  public void testModifyAttributesPersistsTheChange ()
    throws Exception {

    JavaContext context = javaContext(true);
    BasicAttributes addition = new BasicAttributes("description", "modified-by-test");

    context.modifyAttributes("java:beta", DirContext.ADD_ATTRIBUTE, addition);

    Attributes reread = javaContext(false).getAttributes("java:beta");

    Assert.assertNotNull(reread.get("description"), "the added attribute should be visible on a fresh read");
    Assert.assertEquals(reread.get("description").get(), "modified-by-test");
  }

  public void testReadOnlyContextFromTheFactoryRejectsMutation ()
    throws Exception {

    // Proves the CONTEXT_MODIFIABLE=false wiring flows from the factory through to the live context.
    Assert.assertThrows(OperationNotSupportedException.class, () -> javaContext(false).destroySubcontext("java:alpha"));
  }
}
