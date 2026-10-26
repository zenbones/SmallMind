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
package org.smallmind.web.json.scaffold.util;

import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link PolymorphicAttributeScopeManager}, which stores the polymorphic discriminator attribute name in a
 * {@link PerApplicationScope} and falls back to the adapter default when nothing has been stored.
 */
@Test(groups = "unit")
public class PolymorphicAttributeScopeManagerTest {

  public void testDefaultAttributeNameIsReturnedWhenNothingIsStored () {

    Assert.assertEquals(new PerApplicationScope().call(PolymorphicAttributeScopeManager::getPolymorphicAttributeName), AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName());
  }

  public void testSetAndGetRoundTrip () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {
      PolymorphicAttributeScopeManager.setPolymorphicAttributeName("@kind");

      Assert.assertEquals(PolymorphicAttributeScopeManager.getPolymorphicAttributeName(), "@kind");
    });
  }

  public void testSeparateScopesIsolateTheAttributeName () {

    PerApplicationScope first = new PerApplicationScope();
    PerApplicationScope second = new PerApplicationScope();

    first.run(() -> PolymorphicAttributeScopeManager.setPolymorphicAttributeName("@kind"));

    Assert.assertEquals(second.call(PolymorphicAttributeScopeManager::getPolymorphicAttributeName), AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName());
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testGetWithoutScopeThrows () {

    PolymorphicAttributeScopeManager.getPolymorphicAttributeName();
  }
}
