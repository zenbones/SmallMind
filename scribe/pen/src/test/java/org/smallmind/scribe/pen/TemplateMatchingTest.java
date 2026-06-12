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
 * Exercises the {@code matchLogger} priority contract for each {@link Template} subclass. These tests
 * deliberately never call {@code register()}, which would mutate the global {@code LoggerManager}
 * template registry; matching is pure and observable in isolation.
 */
@Test(groups = "unit")
public class TemplateMatchingTest {

  public void testNoMatchSentinel () {

    Assert.assertEquals(Template.NO_MATCH, -1);
  }

  public void testDefaultTemplateMatchesEveryNameAtLowestPriority () {

    DefaultTemplate template = new DefaultTemplate();

    Assert.assertEquals(template.matchLogger("anything.at.all"), Template.NO_MATCH + 1);
    Assert.assertEquals(template.matchLogger("a.different.name"), Template.NO_MATCH + 1);
  }

  public void testClassNameTemplateMatchesByDotNotation ()
    throws LoggerException {

    ClassNameTemplate template = new ClassNameTemplate("com.example.*");

    Assert.assertTrue(template.matchLogger("com.example.Widget") > Template.NO_MATCH);
    Assert.assertEquals(template.matchLogger("org.elsewhere.Widget"), Template.NO_MATCH);
  }

  public void testClassNameTemplateScoresMoreSpecificPatternsHigher ()
    throws LoggerException {

    ClassNameTemplate exact = new ClassNameTemplate("com.example.Widget");
    ClassNameTemplate wildcard = new ClassNameTemplate("com.example.*");

    // Both match the same logger; the more specific (fully literal) pattern must win on priority.
    Assert.assertTrue(exact.matchLogger("com.example.Widget") > wildcard.matchLogger("com.example.Widget"));
  }

  @Test(expectedExceptions = LoggerException.class)
  public void testClassNameTemplateRejectsMalformedPattern ()
    throws LoggerException {

    new ClassNameTemplate(".leading.dot.is.illegal");
  }

  public void testRegExTemplateMatchesAtMaxPriority ()
    throws LoggerException {

    RegExTemplate template = new RegExTemplate(Level.INFO, false, "com\\.example\\..*");

    Assert.assertEquals(template.matchLogger("com.example.Widget"), Integer.MAX_VALUE);
    Assert.assertEquals(template.matchLogger("com.other.Widget"), Template.NO_MATCH);
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testRegExTemplateRejectsSecondInitialization () {

    RegExTemplate template = new RegExTemplate("first");

    template.setExpression("second");
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testRegExTemplateUninitializedMatchThrows () {

    new RegExTemplate().matchLogger("anything");
  }

  public void testPersonalizedTemplateMatchesExactNameOnly () {

    PersonalizedTemplate template = new PersonalizedTemplate("com.example.Widget");

    Assert.assertEquals(template.matchLogger("com.example.Widget"), Integer.MAX_VALUE);
    Assert.assertEquals(template.matchLogger("com.example.Widgets"), Template.NO_MATCH);
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testPersonalizedTemplateRejectsSecondInitialization () {

    PersonalizedTemplate template = new PersonalizedTemplate("first");

    template.setLoggerName("second");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSettingNullLevelThrows () {

    new DefaultTemplate().setLevel(null);
  }
}
