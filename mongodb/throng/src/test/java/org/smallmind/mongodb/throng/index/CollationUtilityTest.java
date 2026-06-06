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
package org.smallmind.mongodb.throng.index;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import org.smallmind.mongodb.throng.index.annotation.Indexes;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CollationUtilityTest {

  private static org.smallmind.mongodb.throng.index.annotation.Collation getCollationFromHolder (Class<?> holderClass) {

    return holderClass.getAnnotation(Indexes.class).options().collation();
  }

  public void testDefaultCollationAnnotationProducesDefaultDriverCollation () {

    Collation collation = CollationUtility.generate(getCollationFromHolder(DefaultHolder.class));

    Assert.assertEquals(collation.getCaseLevel(), Boolean.FALSE);
    Assert.assertEquals(collation.getStrength(), CollationStrength.TERTIARY);
    Assert.assertEquals(collation.getNumericOrdering(), Boolean.FALSE);
    Assert.assertEquals(collation.getBackwards(), Boolean.FALSE);
    Assert.assertEquals(collation.getCaseFirst(), CollationCaseFirst.OFF);
    Assert.assertEquals(collation.getAlternate(), CollationAlternate.NON_IGNORABLE);
    Assert.assertEquals(collation.getMaxVariable(), CollationMaxVariable.PUNCT);
    Assert.assertEquals(collation.getNormalization(), Boolean.FALSE);
  }

  public void testCustomCollationAnnotationPropagatesEveryField () {

    Collation collation = CollationUtility.generate(getCollationFromHolder(CustomHolder.class));

    Assert.assertEquals(collation.getLocale(), "fr_CA");
    Assert.assertEquals(collation.getCaseLevel(), Boolean.TRUE);
    Assert.assertEquals(collation.getStrength(), CollationStrength.SECONDARY);
    Assert.assertEquals(collation.getNumericOrdering(), Boolean.TRUE);
    Assert.assertEquals(collation.getBackwards(), Boolean.TRUE);
    Assert.assertEquals(collation.getCaseFirst(), CollationCaseFirst.UPPER);
    Assert.assertEquals(collation.getAlternate(), CollationAlternate.SHIFTED);
    Assert.assertEquals(collation.getMaxVariable(), CollationMaxVariable.SPACE);
    Assert.assertEquals(collation.getNormalization(), Boolean.TRUE);
  }

  public void testExplicitlyEmptyLocaleAnnotationLeavesLocaleUnsetOnDriverCollation () {

    Collation collation = CollationUtility.generate(getCollationFromHolder(EmptyLocaleHolder.class));

    Assert.assertNull(collation.getLocale());
  }

  @Indexes(value = {})
  private static class DefaultHolder {

  }

  @Indexes(
    value = {},
    options = @org.smallmind.mongodb.throng.index.annotation.IndexOptions(
      collation = @org.smallmind.mongodb.throng.index.annotation.Collation(
        locale = "fr_CA",
        caseLevel = true,
        strength = CollationStrength.SECONDARY,
        numericOrdering = true,
        backwards = true,
        caseFirst = CollationCaseFirst.UPPER,
        alternate = CollationAlternate.SHIFTED,
        maxVariable = CollationMaxVariable.SPACE,
        normalization = true)))
  private static class CustomHolder {

  }

  @Indexes(
    value = {},
    options = @org.smallmind.mongodb.throng.index.annotation.IndexOptions(
      collation = @org.smallmind.mongodb.throng.index.annotation.Collation(locale = "")))
  private static class EmptyLocaleHolder {

  }
}
