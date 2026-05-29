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
package org.smallmind.nutsnbolts.reflection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FieldUtilityTest {

  public void testFieldAccessorsWalkHierarchyAndSkipStaticAndTransient () {

    FieldAccessor[] accessors = FieldUtility.getFieldAccessors(ChildBean.class);
    Set<String> names = new HashSet<>();

    for (FieldAccessor accessor : accessors) {
      names.add(accessor.getName());
    }

    Assert.assertTrue(names.contains("baseField"));
    Assert.assertTrue(names.contains("childField"));
    Assert.assertFalse(names.contains("staticField"));
    Assert.assertFalse(names.contains("transientField"));
  }

  public void testFieldAccessorsAreCached () {

    FieldAccessor[] first = FieldUtility.getFieldAccessors(ChildBean.class);
    FieldAccessor[] second = FieldUtility.getFieldAccessors(ChildBean.class);

    Assert.assertSame(first, second);
  }

  public void testFieldAccessorsSortedAlphaNumerically () {

    FieldAccessor[] accessors = FieldUtility.getFieldAccessors(ChildBean.class);

    String[] sorted = Arrays.stream(accessors).map(FieldAccessor::getName).toArray(String[]::new);

    for (int index = 1; index < sorted.length; index++) {
      Assert.assertTrue(sorted[index - 1].compareTo(sorted[index]) <= 0, "Unsorted: " + Arrays.toString(sorted));
    }
  }

  public void testGetFieldAccessorReturnsMatchOrNull () {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(ChildBean.class, "childField");

    Assert.assertNotNull(accessor);
    Assert.assertEquals(accessor.getName(), "childField");
    Assert.assertNull(FieldUtility.getFieldAccessor(ChildBean.class, "no-such-field"));
  }

  public void testFieldAccessorWiresGetterAndSetterWhenPresent ()
    throws Exception {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(ChildBean.class, "childField");
    ChildBean target = new ChildBean();

    accessor.set(target, "value");

    Assert.assertEquals(accessor.get(target), "value");
  }

  public static class BaseBean {

    private String baseField;

    public String getBaseField () {

      return baseField;
    }

    public void setBaseField (String baseField) {

      this.baseField = baseField;
    }
  }

  public static class ChildBean extends BaseBean {

    private static String staticField = "static";
    private transient String transientField = "transient";
    private String childField;

    public String getChildField () {

      return childField;
    }

    public void setChildField (String childField) {

      this.childField = childField;
    }
  }
}
