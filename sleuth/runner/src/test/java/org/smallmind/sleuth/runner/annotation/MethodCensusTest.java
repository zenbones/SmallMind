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
package org.smallmind.sleuth.runner.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MethodCensusTest {

  private static List<String> tokensFor (Class<?> clazz) {

    List<String> tokens = new ArrayList<>();

    for (Method method : new MethodCensus(clazz)) {
      tokens.add(method.getDeclaringClass().getSimpleName() + "#" + method.getName());
    }

    return tokens;
  }

  public void testSuperclassMethodsArePresentedBeforeSubclassMethods () {

    List<String> tokens = tokensFor(Derived.class);

    int baseShared = tokens.indexOf("Base#shared");
    int derivedShared = tokens.indexOf("Derived#shared");

    Assert.assertTrue(baseShared >= 0, "Base#shared should be visited");
    Assert.assertTrue(derivedShared >= 0, "Derived#shared should be visited");

    // Base-before-leaf ordering is what lets AnnotationMethodology drop the overriding subclass copy:
    // the superclass signature is registered first and the identical subclass signature is ignored.
    Assert.assertTrue(baseShared < derivedShared, "Base methods must precede subclass methods, was " + tokens);
  }

  public void testBothInheritedAndDeclaredMethodsAreEnumerated () {

    List<String> tokens = tokensFor(Derived.class);

    Assert.assertTrue(tokens.contains("Base#baseOnly"), "Inherited declaring class should appear, was " + tokens);
    Assert.assertTrue(tokens.contains("Derived#derivedOnly"), "Leaf-declared method should appear, was " + tokens);
  }

  public void testIteratorThrowsWhenExhausted () {

    Iterator<Method> iterator = new MethodCensus(Tiny.class).iterator();

    while (iterator.hasNext()) {
      iterator.next();
    }

    try {
      iterator.next();
      Assert.fail("Expected a NoSuchElementException past the end of the hierarchy");
    } catch (NoSuchElementException noSuchElementException) {
      // expected
    }
  }

  public void testIteratorRemoveIsUnsupported () {

    Iterator<Method> iterator = new MethodCensus(Base.class).iterator();

    try {
      iterator.remove();
      Assert.fail("Expected an UnsupportedOperationException from a reflection-backed iterator");
    } catch (UnsupportedOperationException unsupportedOperationException) {
      // expected
    }
  }

  public static class Base {

    public void shared () {

    }

    public void baseOnly () {

    }
  }

  public static class Derived extends Base {

    @Override
    public void shared () {

    }

    public void derivedOnly () {

    }
  }

  public static class Tiny {

  }
}
