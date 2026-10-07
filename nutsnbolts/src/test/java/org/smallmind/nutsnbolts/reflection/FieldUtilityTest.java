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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;
import org.smallmind.nutsnbolts.reflection.sample.AccessorBean;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FieldUtilityTest {

  public void testFieldAccessorsWalkHierarchyAndSkipStaticAndTransient ()
    throws IllegalAccessException {

    FieldAccessor[] accessors = FieldUtility.getFieldAccessors(MethodHandles.lookup(), ChildBean.class);
    Set<String> names = new HashSet<>();

    for (FieldAccessor accessor : accessors) {
      names.add(accessor.getName());
    }

    Assert.assertTrue(names.contains("baseField"));
    Assert.assertTrue(names.contains("childField"));
    Assert.assertFalse(names.contains("staticField"));
    Assert.assertFalse(names.contains("transientField"));
  }

  public void testFieldAccessorsAreCachedAndReturnedInNewArrays ()
    throws IllegalAccessException {

    FieldAccessor[] first = FieldUtility.getFieldAccessors(MethodHandles.lookup(), ChildBean.class);
    FieldAccessor[] second;
    FieldAccessor firstAccessor = first[0];

    first[0] = null;
    second = FieldUtility.getFieldAccessors(MethodHandles.lookup(), ChildBean.class);

    Assert.assertNotSame(first, second);
    Assert.assertSame(second[0], firstAccessor);
    Assert.assertSame(FieldUtility.getFieldAccessor(MethodHandles.lookup(), ChildBean.class, firstAccessor.getName()), firstAccessor);
  }

  public void testAccessorsOfClassFromAncestorLoaderAreHeldAgainstLookupClass ()
    throws Exception {

    Path classDirectory = Files.createTempDirectory("field-utility-child");
    Path packageDirectory = Files.createDirectories(classDirectory.resolve("loaded").resolve("child"));
    Path lookupSource = Files.writeString(packageDirectory.resolve("LookupSource.java"), "package loaded.child; import java.lang.invoke.MethodHandles; public class LookupSource { public static MethodHandles.Lookup lookup () { return MethodHandles.lookup(); } }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), lookupSource.toString()), 0);

    try (URLClassLoader childLoader = new URLClassLoader(new URL[] {classDirectory.toUri().toURL()}, FieldUtilityTest.class.getClassLoader())) {

      Class<?> lookupSourceClass = childLoader.loadClass("loaded.child.LookupSource");
      MethodHandles.Lookup childLookup = (MethodHandles.Lookup)lookupSourceClass.getMethod("lookup").invoke(null);
      ClassValue<?> fieldAccessorMapValue = (ClassValue<?>)readStaticField("FIELD_ACCESSOR_MAP_VALUE");
      ClassValue<?> ancestorFieldAccessorMapValue = (ClassValue<?>)readStaticField("ANCESTOR_FIELD_ACCESSOR_MAP_VALUE");

      FieldUtility.getFieldAccessors(childLookup, ChildBean.class);
      FieldUtility.getFieldAccessors(MethodHandles.lookup(), ChildBean.class);

      Assert.assertTrue(((Map<?, ?>)ancestorFieldAccessorMapValue.get(lookupSourceClass)).containsKey(ChildBean.class));
      Assert.assertFalse(((Map<?, ?>)fieldAccessorMapValue.get(ChildBean.class)).containsKey(lookupSourceClass.getModule()));
      Assert.assertTrue(((Map<?, ?>)fieldAccessorMapValue.get(ChildBean.class)).containsKey(FieldUtilityTest.class.getModule()));
    }
  }

  public void testFieldAccessorsSortedAlphaNumerically ()
    throws IllegalAccessException {

    FieldAccessor[] accessors = FieldUtility.getFieldAccessors(MethodHandles.lookup(), ChildBean.class);

    String[] sorted = Arrays.stream(accessors).map(FieldAccessor::getName).toArray(String[]::new);

    for (int index = 1; index < sorted.length; index++) {
      Assert.assertTrue(sorted[index - 1].compareTo(sorted[index]) <= 0, "Unsorted: " + Arrays.toString(sorted));
    }
  }

  public void testGetFieldAccessorReturnsMatchOrNull ()
    throws IllegalAccessException {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), ChildBean.class, "childField");

    Assert.assertNotNull(accessor);
    Assert.assertEquals(accessor.getName(), "childField");
    Assert.assertNull(FieldUtility.getFieldAccessor(MethodHandles.lookup(), ChildBean.class, "no-such-field"));
  }

  public void testFieldAccessorWiresGetterAndSetterWhenPresent ()
    throws Exception {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), ChildBean.class, "childField");
    ChildBean target = new ChildBean();

    accessor.set(target, "value");

    Assert.assertEquals(accessor.get(target), "value");
  }

  public void testNoFieldIsMadeAccessible ()
    throws Exception {

    AccessorBean target = new AccessorBean();
    FieldAccessor describedAccessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), AccessorBean.class, "described");
    FieldAccessor undescribedAccessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), AccessorBean.class, "undescribed");

    Assert.assertFalse(describedAccessor.getField().canAccess(target));
    Assert.assertFalse(undescribedAccessor.getField().canAccess(target));

    describedAccessor.set(target, "through setter");
    undescribedAccessor.set(target, "through field");

    Assert.assertEquals(describedAccessor.get(target), "through setter");
    Assert.assertEquals(undescribedAccessor.get(target), "through field");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*must have full privilege access.*")
  public void testRestrictedLookupIsRejected ()
    throws IllegalAccessException {

    FieldUtility.getFieldAccessors(MethodHandles.publicLookup(), ChildBean.class);
  }

  public void testFinalFieldWithoutSetterIsReadOnly ()
    throws Exception {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), FixedBean.class, "fixed");
    FixedBean target = new FixedBean();

    Assert.assertEquals(accessor.get(target), "fixed");

    try {
      accessor.set(target, "changed");
      Assert.fail("A final field without a setter must not be writable");
    } catch (IllegalAccessException illegalAccessException) {
      Assert.assertTrue(illegalAccessException.getMessage().contains("is 'final' and has no setter"));
    }
  }

  public void testGetterFailureIsWrappedAsInvocationTargetException ()
    throws IllegalAccessException {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), FailingBean.class, "broken");

    try {
      accessor.get(new FailingBean());
      Assert.fail("The getter failure must surface");
    } catch (InvocationTargetException invocationTargetException) {
      Assert.assertTrue(invocationTargetException.getCause() instanceof IllegalStateException);
    }
  }

  public void testPrimitiveFieldIsBoxedAndUnboxed ()
    throws Exception {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(MethodHandles.lookup(), FixedBean.class, "count");
    FixedBean target = new FixedBean();

    accessor.set(target, 42);

    Assert.assertEquals(accessor.get(target), 42);
  }

  private static Object readStaticField (String name)
    throws ReflectiveOperationException {

    Field field = FieldUtility.class.getDeclaredField(name);

    field.setAccessible(true);

    return field.get(null);
  }

  public static class FixedBean {

    private final String fixed = "fixed";
    private int count;
  }

  public static class FailingBean {

    private String broken;

    public String getBroken () {

      throw new IllegalStateException("broken");
    }

    public void setBroken (String broken) {

      this.broken = broken;
    }
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
