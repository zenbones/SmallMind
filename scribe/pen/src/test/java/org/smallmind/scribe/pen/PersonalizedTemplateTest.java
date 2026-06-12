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
package org.smallmind.scribe.pen;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link PersonalizedTemplate}: its exact-name matching at maximum priority, the deferred and
 * constructor-based name binding, the single-initialization guard, and the never-initialized failure.
 * None of these tests call {@code register()}, so the global {@link LoggerManager} is untouched.
 */
@Test(groups = "unit")
public class PersonalizedTemplateTest {

  public void testDeferredNameBindingMatchesExactNameOnly () {

    PersonalizedTemplate template = new PersonalizedTemplate();

    template.setLoggerName("a.b.Exact");

    Assert.assertEquals(template.matchLogger("a.b.Exact"), Integer.MAX_VALUE);
    Assert.assertEquals(template.matchLogger("a.b.Other"), Template.NO_MATCH);
  }

  public void testNameConstructorMatches () {

    PersonalizedTemplate template = new PersonalizedTemplate("x.y.Z");

    Assert.assertEquals(template.matchLogger("x.y.Z"), Integer.MAX_VALUE);
    Assert.assertEquals(template.matchLogger("nope"), Template.NO_MATCH);
  }

  public void testLevelConstructorCarriesLevelAndContext ()
    throws LoggerException {

    PersonalizedTemplate template = new PersonalizedTemplate(Level.DEBUG, true, "lvl.Logger");

    Assert.assertEquals(template.getLevel(), Level.DEBUG);
    Assert.assertTrue(template.isAutoFillLoggerContext());
    Assert.assertEquals(template.matchLogger("lvl.Logger"), Integer.MAX_VALUE);
  }

  public void testFullConstructorPopulatesCollectionsAndName ()
    throws LoggerException {

    PersonalizedTemplate template = new PersonalizedTemplate(new Filter[] {record -> true}, new Appender[] {new CapturingAppender()}, new Enhancer[] {record -> {
    }}, Level.WARN, false, "full.Logger");

    Assert.assertEquals(template.getFilters().length, 1);
    Assert.assertEquals(template.getAppenders().length, 1);
    Assert.assertEquals(template.getEnhancers().length, 1);
    Assert.assertEquals(template.matchLogger("full.Logger"), Integer.MAX_VALUE);
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testSecondNameBindingIsRejected () {

    new PersonalizedTemplate("first.Name").setLoggerName("second.Name");
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testMatchBeforeInitializationThrows () {

    new PersonalizedTemplate().matchLogger("anything");
  }
}
