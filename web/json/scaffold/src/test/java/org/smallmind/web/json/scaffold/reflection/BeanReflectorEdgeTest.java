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
import tools.jackson.core.JacksonException;

/**
 * Exercises the exception, deep-traversal, and ordinal-label branches of {@link BeanReflector} that the
 * happy-path coverage in {@link BeanReflectorTest} and {@link BeanReflectorBranchTest} does not reach:
 * getter/setter/method invocations that throw at runtime (surfacing as {@link BeanAccessException}
 * wrapping the {@code InvocationTargetException}), a mid-chain method call, a public array field updated
 * through a subscript, the null and non-array penultimate failures on subscripted set, an argument that
 * can not be converted to the target parameter type during method matching, and the higher ordinal
 * labels produced by the reflector's {@code indexToNth} rendering ("3rd" and "4th").
 */
@Test(groups = "unit")
public class BeanReflectorEdgeTest {

  public void testConstructable () {

    // The class exposes only static helpers; the default constructor is otherwise never reached.
    Assert.assertNotNull(new BeanReflector());
  }

  public void testGetThroughMidChainMethodCall ()
    throws BeanAccessException {

    // self() returns the same bean, so the chain continues into a further getter.
    Assert.assertEquals(BeanReflector.get(new ReflectorEdgeBean(), "self().label"), "edge");
  }

  public void testGetPublicArrayFieldThroughSubscript ()
    throws BeanAccessException {

    ReflectorEdgeBean bean = new ReflectorEdgeBean();

    bean.publicCells = new int[] {5, 6, 7};

    Assert.assertEquals(BeanReflector.get(bean, "publicCells[2]"), 7);
  }

  public void testSetPublicArrayFieldThroughSubscript ()
    throws BeanAccessException {

    ReflectorEdgeBean bean = new ReflectorEdgeBean();

    bean.publicCells = new int[] {5, 6, 7};

    BeanReflector.set(bean, "publicCells[1]", 99);

    Assert.assertEquals(bean.publicCells[1], 99);
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetThrowingGetterIsWrapped ()
    throws BeanAccessException {

    BeanReflector.get(new ReflectorEdgeBean(), "boom");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetThrowingSetterIsWrapped ()
    throws BeanAccessException {

    BeanReflector.set(new ReflectorEdgeBean(), "boom", "value");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testApplyThrowingMethodIsWrapped ()
    throws BeanAccessException {

    BeanReflector.apply(new ReflectorEdgeBean(), "detonate");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testApplyLastComponentWithArgumentsFails ()
    throws BeanAccessException {

    // The terminal component of an apply chain must not specify an argument list of its own.
    BeanReflector.apply(new ReflectorEdgeBean(), "detonate()");
  }

  @Test(expectedExceptions = JacksonException.class)
  public void testApplyUnconvertibleArgumentPropagatesConversionFailure ()
    throws BeanAccessException {

    // NOTE: executeMethod's argument-conversion catch only handles IllegalArgumentException (the
    // Jackson 2.x conversion-failure type). Under Jackson 3.x, JsonCodec.convert raises a
    // MismatchedInputException (a JacksonException, not an IllegalArgumentException), so the failure
    // is NOT downgraded to a "no matching method" miss but escapes raw. This pins current behavior.
    BeanReflector.apply(new ReflectorEdgeBean(), "needsNumber", new Object());
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetSubscriptThroughThrowingSetterIsWrapped ()
    throws BeanAccessException {

    // The array element is updated through the getter, then setBoomCells throws when invoked.
    BeanReflector.set(new ReflectorEdgeBean(), "boomCells[1]", 9);
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetSubscriptThroughNullPenultimateFails ()
    throws BeanAccessException {

    ReflectorEdgeBean bean = new ReflectorEdgeBean();

    // cube[0][0] is null, so applying the third subscript hits a null penultimate value.
    bean.setCube(new int[][][] {{null}});

    BeanReflector.set(bean, "cube[0][0][0]", 1);
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetSubscriptThroughNonArrayPenultimateFails ()
    throws BeanAccessException {

    ReflectorEdgeBean bean = new ReflectorEdgeBean();

    // words[0] is a String, so the penultimate value of words[0][0] is not an array.
    bean.setWords(new String[] {"hi"});

    BeanReflector.set(bean, "words[0][0]", "x");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetSubscriptBeyondThirdDimensionRendersOrdinal ()
    throws BeanAccessException {

    ReflectorEdgeBean bean = new ReflectorEdgeBean();

    // cube is three-dimensional, so the fourth subscript dereferences an int and fails at the "4th" ordinal.
    bean.setCube(new int[][][] {{{5}}});

    BeanReflector.get(bean, "cube[0][0][0][0]");
  }
}
