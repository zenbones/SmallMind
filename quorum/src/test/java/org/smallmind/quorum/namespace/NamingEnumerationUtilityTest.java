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
import javax.naming.InvalidNameException;
import org.smallmind.quorum.namespace.NamespaceTestSupport.RecordingDirContext;
import org.smallmind.quorum.namespace.NamespaceTestSupport.StubNameTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the package-private {@link NamingEnumerationUtility} conversions used while
 * translating backing-store naming enumeration elements.
 */
@Test(groups = "unit")
public class NamingEnumerationUtilityTest {

  private final StubNameTranslator translator = new StubNameTranslator();

  public void testConvertNameDelegatesToTheTranslator ()
    throws InvalidNameException {

    Assert.assertEquals(NamingEnumerationUtility.convertName("CN=Foo", translator), "cn=foo");
  }

  public void testConvertClassNameRewritesTheBackingContextClass () {

    Assert.assertEquals(NamingEnumerationUtility.convertClassName(RecordingDirContext.class.getName(), RecordingDirContext.class), JavaContext.class.getName());
  }

  public void testConvertClassNameLeavesForeignClassNamesUntouched () {

    Assert.assertEquals(NamingEnumerationUtility.convertClassName("java.lang.String", RecordingDirContext.class), "java.lang.String");
  }

  public void testConvertClassNamePassesNullThrough () {

    Assert.assertNull(NamingEnumerationUtility.convertClassName(null, RecordingDirContext.class));
  }

  public void testConvertObjectWrapsABackingDirectoryContext () {

    Object converted = NamingEnumerationUtility.convertObject(new RecordingDirContext(), RecordingDirContext.class, new Hashtable<>(), translator, new JavaNameParser(translator), false);

    Assert.assertTrue(converted instanceof JavaContext, "a backing directory context should be wrapped as a JavaContext");
  }

  public void testConvertObjectLeavesForeignObjectsUntouched () {

    Object value = "a-value";

    Assert.assertSame(NamingEnumerationUtility.convertObject(value, RecordingDirContext.class, new Hashtable<>(), translator, new JavaNameParser(translator), false), value);
  }

  public void testConvertObjectPassesNullThrough () {

    Assert.assertNull(NamingEnumerationUtility.convertObject(null, RecordingDirContext.class, new Hashtable<>(), translator, new JavaNameParser(translator), false));
  }
}
