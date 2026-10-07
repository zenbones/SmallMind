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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FieldAccessorTest {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  public void testFieldHandlesReadAndWriteTheFieldDirectly ()
    throws Exception {

    FieldAccessor accessor = new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "name", String.class), LOOKUP.findSetter(Bean.class, "name", String.class));
    Bean target = new Bean();

    accessor.set(target, "direct");

    Assert.assertEquals(accessor.get(target), "direct");
    Assert.assertEquals(target.name, "direct");
  }

  public void testFailureOfDirectFieldAccessIsNotWrapped ()
    throws Exception {

    FieldAccessor accessor = new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "name", String.class), null);

    Assert.assertThrows(NullPointerException.class, () -> accessor.get(null));
  }

  public void testFailureOfGetterMethodIsWrapped ()
    throws Exception {

    FieldAccessor accessor = new FieldAccessor(nameField(), LOOKUP.findVirtual(Bean.class, "getName", MethodType.methodType(String.class)), null);

    try {
      accessor.get(new Bean());
      Assert.fail("Expected the getter failure to surface");
    } catch (InvocationTargetException invocationTargetException) {
      Assert.assertTrue(invocationTargetException.getCause() instanceof IllegalStateException);
    }
  }

  public void testSetterMethodWithBoxedParameterIsAccepted ()
    throws Exception {

    Field countField = Bean.class.getDeclaredField("count");
    FieldAccessor accessor = new FieldAccessor(countField, LOOKUP.findGetter(Bean.class, "count", int.class), LOOKUP.findVirtual(Bean.class, "setCount", MethodType.methodType(Bean.class, Integer.class)));
    Bean target = new Bean();

    accessor.set(target, 3);

    Assert.assertEquals(accessor.get(target), 3);
  }

  public void testGetterMethodOfUnimplementedInterfaceIsAccepted ()
    throws Exception {

    Assert.assertNotNull(new FieldAccessor(nameField(), LOOKUP.findVirtual(Named.class, "getName", MethodType.methodType(String.class)), null));
  }

  public void testAccessorWithoutSetterCannotWrite ()
    throws Exception {

    FieldAccessor accessor = new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "name", String.class), null);

    try {
      accessor.set(new Bean(), "unwritable");
      Assert.fail("Expected an accessor without a setter to refuse the write");
    } catch (IllegalAccessException illegalAccessException) {
      Assert.assertEquals(illegalAccessException.getMessage(), "The field(name) of class(" + Bean.class.getName() + ") has no setter");
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*must not be 'static'")
  public void testStaticFieldIsRejected ()
    throws Exception {

    new FieldAccessor(Bean.class.getDeclaredField("shared"), LOOKUP.findStaticGetter(Bean.class, "shared", String.class), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*requires a getter handle")
  public void testMissingGetterIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), null, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The getter handle .* must be a direct method handle.*")
  public void testAdaptedHandleIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "name", String.class).asType(MethodType.methodType(Object.class, Bean.class)), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The getter handle .* must read that field, not access .*Bean\\.other")
  public void testGetterOfAnotherFieldIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "other", String.class), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The getter handle .* must read that field, not access .*Bean\\.name")
  public void testSetterHandlePassedAsGetterIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findSetter(Bean.class, "name", String.class), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The getter handle .* must invoke an instance method that takes no arguments and returns a value, not .*")
  public void testStaticGetterMethodIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findStatic(Bean.class, "sharedName", MethodType.methodType(String.class)), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The setter handle .* must invoke an instance method that takes one argument accepting the type\\(java\\.lang\\.String\\), not .*")
  public void testSetterMethodWithIncompatibleParameterIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findGetter(Bean.class, "name", String.class), LOOKUP.findVirtual(Bean.class, "setCount", MethodType.methodType(Bean.class, Integer.class)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "The getter handle .* takes a receiver of type\\(.*Unrelated\\), which is unrelated to the class declaring the field")
  public void testUnrelatedReceiverIsRejected ()
    throws Exception {

    new FieldAccessor(nameField(), LOOKUP.findVirtual(Unrelated.class, "getName", MethodType.methodType(String.class)), null);
  }

  public void testFieldUtilityIgnoresGetterReturningVoid ()
    throws Exception {

    FieldAccessor accessor = FieldUtility.getFieldAccessor(LOOKUP, VoidGetterBean.class, "state");
    VoidGetterBean target = new VoidGetterBean();

    accessor.set(target, "read directly");

    Assert.assertEquals(accessor.get(target), "read directly");
  }

  private static Field nameField ()
    throws NoSuchFieldException {

    return Bean.class.getDeclaredField("name");
  }

  public interface Named {

    default String getName () {

      return "named";
    }
  }

  public static class Bean {

    private static String shared;
    private String name;
    private String other;
    private int count;

    public static String sharedName () {

      return shared;
    }

    public String getName () {

      throw new IllegalStateException("broken");
    }

    public Bean setCount (Integer count) {

      this.count = count;

      return this;
    }
  }

  public static class Unrelated {

    public String getName () {

      return "unrelated";
    }
  }

  public static class VoidGetterBean {

    private String state;

    public void getState () {

    }
  }
}
