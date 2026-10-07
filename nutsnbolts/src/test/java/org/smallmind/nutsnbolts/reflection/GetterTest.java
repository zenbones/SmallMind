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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GetterTest {

  public void testGetPrefixedMethodParsesAttribute ()
    throws Exception {

    Getter getter = new Getter(Bean.class.getDeclaredMethod("getName"));

    Assert.assertFalse(getter.isIs());
    Assert.assertEquals(getter.getAttributeName(), "name");
    Assert.assertEquals(getter.getAttributeClass(), String.class);
  }

  public void testIsPrefixedBooleanMethodParsesAttribute ()
    throws Exception {

    Getter getter = new Getter(Bean.class.getDeclaredMethod("isReady"));

    Assert.assertTrue(getter.isIs());
    Assert.assertEquals(getter.getAttributeName(), "ready");
    Assert.assertEquals(getter.getAttributeClass(), Boolean.class);
  }

  public void testInvokeReturnsAttributeValue ()
    throws Exception {

    Getter getter = new Getter(Bean.class.getDeclaredMethod("getName"));

    Assert.assertEquals(getter.invoke(new Bean()), "Berkman");
  }

  public void testInvokeOnIsGetterReturnsBoxedBoolean ()
    throws Exception {

    Getter getter = new Getter(Bean.class.getDeclaredMethod("isReady"));

    Assert.assertEquals(getter.invoke(new Bean()), Boolean.TRUE);
  }

  public void testIsPrefixedPrimitiveBooleanMethodParsesAttribute ()
    throws Exception {

    Getter getter = new Getter(Bean.class.getDeclaredMethod("isActive"));

    Assert.assertTrue(getter.isIs());
    Assert.assertEquals(getter.getAttributeName(), "active");
    Assert.assertEquals(getter.getAttributeClass(), boolean.class);
  }

  @Test(expectedExceptions = ReflectionContractException.class)
  public void testVoidGetterIsRejected ()
    throws Exception {

    new Getter(Bean.class.getDeclaredMethod("getNothing"));
  }

  @Test(expectedExceptions = ReflectionContractException.class)
  public void testIsPrefixedNonBooleanMethodIsRejected ()
    throws Exception {

    new Getter(Bean.class.getDeclaredMethod("isNamed"));
  }

  public void testSerializedGetterInvokesWrappedMethod ()
    throws Exception {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(new Getter(Bean.class.getDeclaredMethod("getName")));
    }

    try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {

      Getter getter = (Getter)objectInputStream.readObject();

      Assert.assertEquals(getter.getAttributeName(), "name");
      Assert.assertEquals(getter.invoke(new Bean()), "Berkman");
    }
  }

  @Test(expectedExceptions = ReflectionContractException.class)
  public void testMethodWithNeitherGetNorIsPrefixIsRejected ()
    throws Exception {

    new Getter(Bean.class.getDeclaredMethod("notAGetter"));
  }

  @Test(expectedExceptions = ReflectionContractException.class)
  public void testGetterWithParameterIsRejected ()
    throws Exception {

    new Getter(Bean.class.getDeclaredMethod("getWithArgument", String.class));
  }

  static class Bean {

    public String getName () {

      return "Berkman";
    }

    public Boolean isReady () {

      return Boolean.TRUE;
    }

    public boolean isActive () {

      return true;
    }

    public void getNothing () {

    }

    public String isNamed () {

      return "no";
    }

    public String notAGetter () {

      return "no";
    }

    public String getWithArgument (String unused) {

      return "no";
    }
  }
}
