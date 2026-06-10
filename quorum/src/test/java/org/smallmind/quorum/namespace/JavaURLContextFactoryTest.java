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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link JavaURLContextFactory} construction without contacting any backing store: the
 * factory only resolves the LDAP {@code ContextCreator}/{@code NameTranslator} classes reflectively
 * and stashes the connection details, so a top-level {@link JavaContext} can be built from a plain
 * environment with no live server. The {@code ldap} store identifier is used because it is the only
 * {@link org.smallmind.quorum.namespace.backingStore.StorageType} the repository ships.
 */
@Test(groups = "unit")
public class JavaURLContextFactoryTest {

  private Hashtable<String, Object> ldapEnvironment () {

    Hashtable<String, Object> environment = new Hashtable<>();

    environment.put(JavaContext.CONTEXT_STORE, "ldap");
    environment.put(JavaContext.CONNECTION_DETAILS, new NamingConnectionDetails("localhost", 389, false, "dc=smallmind,dc=org", "cn=Directory Manager", "password"));

    return environment;
  }

  public void testNonNullObjectDefersByReturningNull ()
    throws Exception {

    // The ObjectFactory contract: when an existing object is offered the factory must not build a
    // new context, so it returns null to let the existing object stand.
    Assert.assertNull(new JavaURLContextFactory().getObjectInstance("an-existing-object", null, null, ldapEnvironment()));
  }

  public void testDefaultEnvironmentBuildsAReadOnlyUnpooledContext ()
    throws Exception {

    // Neither CONTEXT_MODIFIABLE nor POOLED_CONNECTION is set, exercising both default-false branches.
    Object instance = new JavaURLContextFactory().getObjectInstance(null, null, null, ldapEnvironment());

    Assert.assertTrue(instance instanceof JavaContext, "a null object should yield a freshly built top-level JavaContext");
  }

  public void testModifiableAndPooledFlagsAreHonoured ()
    throws Exception {

    Hashtable<String, Object> environment = ldapEnvironment();

    environment.put(JavaContext.CONTEXT_MODIFIABLE, "true");
    environment.put(JavaContext.POOLED_CONNECTION, "true");

    Assert.assertTrue(new JavaURLContextFactory().getObjectInstance(null, null, null, environment) instanceof JavaContext);
  }

  public void testNonTrueFlagValuesLeaveTheDefaultsInPlace ()
    throws Exception {

    // The keys are present but not equal to "true", exercising the contains-but-not-"true" branches.
    Hashtable<String, Object> environment = ldapEnvironment();

    environment.put(JavaContext.CONTEXT_MODIFIABLE, "false");
    environment.put(JavaContext.POOLED_CONNECTION, "no");

    Assert.assertTrue(new JavaURLContextFactory().getObjectInstance(null, null, null, environment) instanceof JavaContext);
  }

  public void testGetInitialContextDelegatesToGetObjectInstance ()
    throws NamingException {

    Context context = new JavaURLContextFactory().getInitialContext(ldapEnvironment());

    Assert.assertTrue(context instanceof JavaContext, "as an InitialContextFactory it should build the same top-level context");
  }

  public void testGetInitialContextWrapsAResolutionFailureAsANamingException () {

    // An unknown backing-store identifier makes the reflective Class.forName fail; getInitialContext
    // must convert that non-naming exception into a NamingException carrying the original as root cause.
    Hashtable<String, Object> environment = ldapEnvironment();

    environment.put(JavaContext.CONTEXT_STORE, "bogus");

    NamingException namingException = Assert.expectThrows(NamingException.class, () -> new JavaURLContextFactory().getInitialContext(environment));

    Assert.assertNotNull(namingException.getRootCause(), "the underlying reflective failure should be retained as the root cause");
  }

  public void testGetObjectInstancePropagatesTheRawResolutionFailure () {

    // getObjectInstance, unlike getInitialContext, does not wrap — the reflective failure surfaces raw.
    Hashtable<String, Object> environment = ldapEnvironment();

    environment.put(JavaContext.CONTEXT_STORE, "bogus");

    Assert.assertThrows(Exception.class, () -> new JavaURLContextFactory().getObjectInstance(null, null, null, environment));
  }
}
