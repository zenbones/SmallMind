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
package org.smallmind.nutsnbolts.reflection.type;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TypeUtilityTest {

  public void testIsEssentiallyPrimitiveAcceptsPrimitivesAndWrappers () {

    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(long.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Long.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(boolean.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Boolean.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(int.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Integer.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(double.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Double.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(float.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Float.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(char.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Character.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(short.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Short.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(byte.class));
    Assert.assertTrue(TypeUtility.isEssentiallyPrimitive(Byte.class));
  }

  public void testIsEssentiallyPrimitiveRejectsReferenceTypes () {

    Assert.assertFalse(TypeUtility.isEssentiallyPrimitive(String.class));
    Assert.assertFalse(TypeUtility.isEssentiallyPrimitive(Object.class));
  }

  public void testIsEssentiallyTheSameAsTreatsPrimitivesAndWrappersAsEquivalent () {

    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(long.class, Long.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(Long.class, long.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(boolean.class, Boolean.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(int.class, Integer.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(double.class, Double.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(float.class, Float.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(char.class, Character.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(short.class, Short.class));
    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(byte.class, Byte.class));
  }

  public void testIsEssentiallyTheSameAsRejectsCrossWrapperMismatch () {

    Assert.assertFalse(TypeUtility.isEssentiallyTheSameAs(long.class, Integer.class));
    Assert.assertFalse(TypeUtility.isEssentiallyTheSameAs(Integer.class, long.class));
  }

  public void testIsEssentiallyTheSameAsFallsBackToIsAssignableFromForReferenceTypes () {

    Assert.assertTrue(TypeUtility.isEssentiallyTheSameAs(CharSequence.class, String.class));
    Assert.assertFalse(TypeUtility.isEssentiallyTheSameAs(String.class, CharSequence.class));
    Assert.assertFalse(TypeUtility.isEssentiallyTheSameAs(Number.class, String.class));
  }

  public void testGetDefaultValueProducesZeroOrFalseForPrimitivesAndWrappers () {

    Assert.assertEquals(TypeUtility.getDefaultValue(long.class), 0L);
    Assert.assertEquals(TypeUtility.getDefaultValue(Long.class), 0L);
    Assert.assertEquals(TypeUtility.getDefaultValue(boolean.class), false);
    Assert.assertEquals(TypeUtility.getDefaultValue(Boolean.class), false);
    Assert.assertEquals(TypeUtility.getDefaultValue(int.class), 0);
    Assert.assertEquals(TypeUtility.getDefaultValue(Integer.class), 0);
    Assert.assertEquals(TypeUtility.getDefaultValue(double.class), 0.0D);
    Assert.assertEquals(TypeUtility.getDefaultValue(Double.class), 0.0D);
    Assert.assertEquals(TypeUtility.getDefaultValue(float.class), 0.0F);
    Assert.assertEquals(TypeUtility.getDefaultValue(Float.class), 0.0F);
    Assert.assertEquals(TypeUtility.getDefaultValue(char.class), (char)0);
    Assert.assertEquals(TypeUtility.getDefaultValue(Character.class), (char)0);
    Assert.assertEquals(TypeUtility.getDefaultValue(short.class), 0);
    Assert.assertEquals(TypeUtility.getDefaultValue(Short.class), 0);
    Assert.assertEquals(TypeUtility.getDefaultValue(byte.class), 0);
    Assert.assertEquals(TypeUtility.getDefaultValue(Byte.class), 0);
  }

  public void testGetDefaultValueReturnsNullForReferenceTypes () {

    Assert.assertNull(TypeUtility.getDefaultValue(String.class));
    Assert.assertNull(TypeUtility.getDefaultValue(Object.class));
  }
}
