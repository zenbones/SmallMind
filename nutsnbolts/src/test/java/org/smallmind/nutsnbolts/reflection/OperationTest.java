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

import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OperationTest {

  static class Fixture {

    public void noArgs () {

    }

    public void oneArg (String value) {

    }

    public void twoArgs (String value, int count) {

    }
  }

  private static Method findMethod (String name, Class<?>... parameters)
    throws NoSuchMethodException {

    return Fixture.class.getDeclaredMethod(name, parameters);
  }

  public void testEqualMethodReferencesProduceEqualOperations ()
    throws NoSuchMethodException {

    Operation first = new Operation(findMethod("oneArg", String.class));
    Operation second = new Operation(findMethod("oneArg", String.class));

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
  }

  public void testExplicitConstructorMatchesReflectiveConstructor ()
    throws NoSuchMethodException {

    Operation reflective = new Operation(findMethod("oneArg", String.class));
    Operation explicit = new Operation("oneArg", new String[] {"java.lang.String"});

    Assert.assertEquals(reflective, explicit);
    Assert.assertEquals(reflective.hashCode(), explicit.hashCode());
  }

  public void testGettersExposeConstructorArguments () {

    Operation operation = new Operation("doStuff", new String[] {"java.lang.String", "int"});

    Assert.assertEquals(operation.getOperationName(), "doStuff");
    Assert.assertEquals(operation.getSignatureNames(), new String[] {"java.lang.String", "int"});
  }

  public void testNameMismatchYieldsInequality () {

    Operation lhs = new Operation("foo", new String[] {"java.lang.String"});
    Operation rhs = new Operation("bar", new String[] {"java.lang.String"});

    Assert.assertNotEquals(lhs, rhs);
  }

  public void testArityMismatchYieldsInequality () {

    Operation lhs = new Operation("foo", new String[] {"java.lang.String"});
    Operation rhs = new Operation("foo", new String[] {"java.lang.String", "int"});

    Assert.assertNotEquals(lhs, rhs);
  }

  public void testSignatureMismatchYieldsInequality () {

    Operation lhs = new Operation("foo", new String[] {"java.lang.String", "int"});
    Operation rhs = new Operation("foo", new String[] {"java.lang.String", "long"});

    Assert.assertNotEquals(lhs, rhs);
  }

  public void testNoArgOperationsCompareEqually ()
    throws NoSuchMethodException {

    Operation lhs = new Operation(findMethod("noArgs"));
    Operation rhs = new Operation(findMethod("noArgs"));

    Assert.assertEquals(lhs, rhs);
    Assert.assertEquals(lhs.hashCode(), rhs.hashCode());
    Assert.assertEquals(lhs.getSignatureNames().length, 0);
  }

  public void testEqualsAgainstNonOperationIsFalse () {

    Operation operation = new Operation("foo", new String[0]);

    Assert.assertNotEquals(operation, "foo");
    Assert.assertNotEquals(operation, null);
  }
}
