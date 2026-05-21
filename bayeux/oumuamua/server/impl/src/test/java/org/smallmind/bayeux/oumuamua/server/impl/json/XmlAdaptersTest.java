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
package org.smallmind.bayeux.oumuamua.server.impl.json;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class XmlAdaptersTest {

  // --- ClassNameXmlAdapter ---

  public void testClassNameAdapterMarshalReturnsFullyQualifiedClassName () {

    ClassNameXmlAdapter adapter = new ClassNameXmlAdapter();

    Assert.assertEquals(adapter.marshal("hello"), String.class.getName());
    Assert.assertEquals(adapter.marshal(42), Integer.class.getName());
    Assert.assertEquals(adapter.marshal(adapter), ClassNameXmlAdapter.class.getName());
  }

  public void testClassNameAdapterMarshalNullReturnsNull () {

    Assert.assertNull(new ClassNameXmlAdapter().marshal(null));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testClassNameAdapterUnmarshalThrows () {

    new ClassNameXmlAdapter().unmarshal("java.lang.String");
  }

  // --- ClassNameArrayXmlAdapter ---

  public void testClassNameArrayAdapterMarshalNullReturnsNull () {

    Assert.assertNull(new ClassNameArrayXmlAdapter().marshal(null));
  }

  public void testClassNameArrayAdapterMarshalReturnsClassNames () {

    Object[] input = new Object[] {"hello", 42};
    String result = new ClassNameArrayXmlAdapter().marshal(input);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains(String.class.getName()));
    Assert.assertTrue(result.contains(Integer.class.getName()));
    Assert.assertTrue(result.startsWith("["));
    Assert.assertTrue(result.endsWith("]"));
  }

  public void testClassNameArrayAdapterMarshalEmptyArrayReturnsEmptyBrackets () {

    String result = new ClassNameArrayXmlAdapter().marshal(new Object[0]);

    Assert.assertEquals(result, "[]");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testClassNameArrayAdapterUnmarshalThrows () {

    new ClassNameArrayXmlAdapter().unmarshal("[java.lang.String]");
  }

  // --- DoubleStringArrayXmlAdapter ---

  public void testDoubleStringArrayAdapterMarshalNullReturnsNull () {

    Assert.assertNull(new DoubleStringArrayXmlAdapter().marshal(null));
  }

  public void testDoubleStringArrayAdapterMarshalEmptyArrayReturnsEmptyBrackets () {

    String result = new DoubleStringArrayXmlAdapter().marshal(new String[0][]);

    Assert.assertEquals(result, "[]");
  }

  public void testDoubleStringArrayAdapterMarshalProducesSlashJoinedPaths () {

    String[][] input = new String[][] {{"foo", "bar"}, {"baz"}};
    String result = new DoubleStringArrayXmlAdapter().marshal(input);

    Assert.assertEquals(result, "[/foo/bar,/baz]");
  }

  public void testDoubleStringArrayAdapterMarshalSingleEntry () {

    String[][] input = new String[][] {{"streaming", "**"}};
    String result = new DoubleStringArrayXmlAdapter().marshal(input);

    Assert.assertEquals(result, "[/streaming/**]");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testDoubleStringArrayAdapterUnmarshalThrows () {

    new DoubleStringArrayXmlAdapter().unmarshal("[/foo/bar]");
  }
}
