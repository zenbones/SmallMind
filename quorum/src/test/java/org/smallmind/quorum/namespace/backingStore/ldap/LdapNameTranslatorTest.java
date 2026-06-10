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

import javax.naming.InvalidNameException;
import org.smallmind.quorum.namespace.JavaName;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LdapNameTranslatorTest {

  // Only getRoot() is consulted by the translation methods, so no live LDAP connection is ever opened.
  private static final String ROOT = "cn=root";

  private final LdapNameTranslator translator = new LdapNameTranslator(new LdapContextCreator(new NamingConnectionDetails("localhost", 389, false, ROOT, "user", "secret")));

  private JavaName javaName (String... components)
    throws InvalidNameException {

    JavaName name = new JavaName(translator);

    for (String component : components) {
      name.add(component);
    }

    return name;
  }

  public void testInternalNameIsPrefixedWithCommonNameInOrder ()
    throws InvalidNameException {

    JavaName external = translator.fromInternalNameToExternalName(javaName("a", "b", "c"));

    Assert.assertEquals(external.size(), 3);
    Assert.assertEquals(external.get(0), "cn=a");
    Assert.assertEquals(external.get(1), "cn=b");
    Assert.assertEquals(external.get(2), "cn=c");
  }

  public void testExternalNameRendersAsAReverseOrderedDistinguishedName ()
    throws InvalidNameException {

    Assert.assertEquals(translator.fromExternalNameToExternalString(javaName("cn=a", "cn=b", "cn=c")), "cn=c,cn=b,cn=a");
  }

  public void testRelativeDistinguishedNameDecodesAndReverses ()
    throws InvalidNameException {

    Assert.assertEquals(translator.fromExternalStringToInternalString("cn=c,cn=b,cn=a"), "a/b/c");
  }

  public void testAbsoluteDistinguishedNameStripsRootAndPrependsJavaScheme ()
    throws InvalidNameException {

    Assert.assertEquals(translator.fromAbsoluteExternalStringToInternalString("cn=c,cn=b,cn=a,cn=root"), "java:a/b/c");
  }

  @Test(expectedExceptions = InvalidNameException.class)
  public void testAbsoluteNameEqualToTheRootIsRejected ()
    throws InvalidNameException {

    translator.fromAbsoluteExternalStringToInternalString(ROOT);
  }

  @Test(expectedExceptions = InvalidNameException.class)
  public void testComponentLackingAnEqualsSeparatorIsRejected ()
    throws InvalidNameException {

    translator.fromExternalStringToInternalString("cn=a,malformed");
  }

  public void testInternalToExternalAndBackIsLossless ()
    throws InvalidNameException {

    JavaName external = translator.fromInternalNameToExternalName(javaName("a", "b", "c"));
    String distinguishedName = translator.fromExternalNameToExternalString(external);

    Assert.assertEquals(translator.fromExternalStringToInternalString(distinguishedName), "a/b/c");
  }
}
