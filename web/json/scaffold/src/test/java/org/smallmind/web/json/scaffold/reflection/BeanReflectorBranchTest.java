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
package org.smallmind.web.json.scaffold.reflection;

import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the lesser-travelled branches of {@link BeanReflector} over public top-level bean fixtures:
 * read-only and write-only properties, inherited properties, the {@code isX} boolean accessor and its
 * non-boolean rejection, public-field fallback for both get and set, subscript assignment through both
 * setter methods and public fields, zero-argument method invocation, and the null/non-array/out-of-range
 * subscript failure paths (which drive the {@code indexToNth} ordinal rendering).
 */
@Test(groups = "unit")
public class BeanReflectorBranchTest {

  private ReflectedBean buildBean () {

    ReflectedBean bean = new ReflectedBean();

    bean.setEnabled(true);
    bean.setLabel("start");
    bean.setInherited("base");
    bean.setCells(new int[] {10, 20, 30});
    bean.setGrid(new int[][] {{1, 2}, {3, 4}});
    bean.publicField = "raw";

    return bean;
  }

  public void testGetReadOnlyProperty ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildBean(), "readOnly"), "fixed");
  }

  public void testGetInheritedProperty ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildBean(), "inherited"), "base");
  }

  public void testGetBooleanIsAccessor ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildBean(), "enabled"), Boolean.TRUE);
  }

  public void testGetPublicFieldFallback ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildBean(), "publicField"), "raw");
  }

  public void testGetThroughTwoSubscripts ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildBean(), "grid[1][0]"), 3);
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetIsAccessorWithNonBooleanReturnFails ()
    throws BeanAccessException {

    // isMislabeled() exists but returns a String, so the 'is' accessor is rejected.
    BeanReflector.get(buildBean(), "mislabeled");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetOnNullTargetFails ()
    throws BeanAccessException {

    BeanReflector.get(null, "label");
  }

  public void testSetThroughSetterMethod ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();

    BeanReflector.set(bean, "label", "changed");

    Assert.assertEquals(bean.getLabel(), "changed");
  }

  public void testSetWriteOnlyProperty ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();

    BeanReflector.set(bean, "writeOnly", 17);

    Assert.assertEquals(bean.getWriteOnly(), 17);
  }

  public void testSetNullValueThroughSetter ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();

    BeanReflector.set(bean, "label", null);

    Assert.assertNull(bean.getLabel());
  }

  public void testSetThroughSubscriptOnSetterBackedArray ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();

    BeanReflector.set(bean, "cells[1]", 99);

    Assert.assertEquals(bean.getCells()[1], 99);
  }

  public void testSetPublicFieldFallback ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();

    BeanReflector.set(bean, "publicField", "written");

    Assert.assertEquals(bean.publicField, "written");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetMissingPropertyFails ()
    throws BeanAccessException {

    BeanReflector.set(buildBean(), "noSuchProperty", "x");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetOnNullTargetFails ()
    throws BeanAccessException {

    BeanReflector.set(null, "label", "x");
  }

  public void testApplyZeroArgumentMethod ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.apply(buildBean(), "ping"), "pong");
  }

  public void testApplyMethodWithArgument ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.apply(buildBean(), "echo", "hi"), "echo:hi");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testApplyOnNullTargetFails ()
    throws BeanAccessException {

    BeanReflector.apply(null, "ping");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSubscriptOnNullValueFails ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();
    bean.setCells(null);

    BeanReflector.get(bean, "cells[0]");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSubscriptOnNonArrayValueFails ()
    throws BeanAccessException {

    // 'label' is a String, not an array, so applying a subscript must fail.
    BeanReflector.get(buildBean(), "label[0]");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSubscriptOutOfRangeFails ()
    throws BeanAccessException {

    BeanReflector.get(buildBean(), "cells[9]");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetLastComponentWithArgumentsFails ()
    throws BeanAccessException {

    // The terminal component of a get chain must not specify an argument list.
    BeanReflector.get(buildBean(), "echo(\"hi\")");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetLastComponentWithArgumentsFails ()
    throws BeanAccessException {

    BeanReflector.set(buildBean(), "echo(\"hi\")", "x");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testNestedSubscriptOnNullPenultimateFails ()
    throws BeanAccessException {

    ReflectedBean bean = buildBean();
    bean.setGrid(null);

    BeanReflector.set(bean, "grid[0][0]", 5);
  }
}
