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
package org.smallmind.nutsnbolts.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AnnotationLiteralTest {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Sample {

    String value () default "default";
  }

  public void testAnnotationTypeIsResolved () {

    SampleLiteral literal = new SampleLiteral("x");

    Assert.assertEquals(literal.annotationType(), Sample.class);
  }

  public void testEqualsTrueForSameValues () {

    SampleLiteral a = new SampleLiteral("x");
    SampleLiteral b = new SampleLiteral("x");

    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testEqualsFalseForDifferentValues () {

    SampleLiteral a = new SampleLiteral("x");
    SampleLiteral b = new SampleLiteral("y");

    Assert.assertNotEquals(a, b);
  }

  public void testToStringIncludesMembers () {

    SampleLiteral literal = new SampleLiteral("payload");

    Assert.assertTrue(literal.toString().contains("value=payload"));
  }

  public static class SampleLiteral extends AnnotationLiteral<Sample> implements Sample {

    private final String value;

    public SampleLiteral (String value) {

      this.value = value;
    }

    @Override
    public String value () {

      return value;
    }
  }
}
