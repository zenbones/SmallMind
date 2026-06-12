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
 * Covers the {@link Template} configuration surface (filter/appender/enhancer mutators, constructors,
 * level and auto-fill accessors) through the concrete {@link DefaultTemplate}. None of these tests
 * call {@code register()}, so the templates stay unregistered and never touch the global
 * {@link LoggerManager} — exercising the {@code if (registered)} false branches throughout.
 */
@Test(groups = "unit")
public class TemplateConfigurationTest {

  public void testNoArgConstructorDefaults () {

    DefaultTemplate template = new DefaultTemplate();

    Assert.assertEquals(template.getLevel(), Level.INFO);
    Assert.assertFalse(template.isAutoFillLoggerContext());
    Assert.assertEquals(template.getFilters().length, 0);
    Assert.assertEquals(template.getAppenders().length, 0);
    Assert.assertEquals(template.getEnhancers().length, 0);
  }

  public void testLevelAndContextConstructor () {

    DefaultTemplate template = new DefaultTemplate(Level.DEBUG, true);

    Assert.assertEquals(template.getLevel(), Level.DEBUG);
    Assert.assertTrue(template.isAutoFillLoggerContext());
  }

  public void testAppenderVarargConstructorPopulatesAppenders () {

    DefaultTemplate template = new DefaultTemplate(Level.INFO, false, new CapturingAppender(), new CapturingAppender());

    Assert.assertEquals(template.getAppenders().length, 2);
  }

  public void testFullConstructorPopulatesEveryCollection () {

    DefaultTemplate template = new DefaultTemplate(new Filter[] {record -> true}, new Appender[] {new CapturingAppender()}, new Enhancer[] {record -> {
    }}, Level.WARN, true);

    Assert.assertEquals(template.getFilters().length, 1);
    Assert.assertEquals(template.getAppenders().length, 1);
    Assert.assertEquals(template.getEnhancers().length, 1);
    Assert.assertEquals(template.getLevel(), Level.WARN);
  }

  public void testAddAndRemoveFilter () {

    DefaultTemplate template = new DefaultTemplate();
    Filter first = record -> true;
    Filter second = record -> false;

    template.addFilter(first);
    template.addFilter(second);
    Assert.assertEquals(template.getFilters().length, 2);

    template.removeFilter(first);
    Filter[] remaining = template.getFilters();
    Assert.assertEquals(remaining.length, 1);
    Assert.assertSame(remaining[0], second);
  }

  public void testSetFilterReplacesWithSingle () {

    DefaultTemplate template = new DefaultTemplate();

    template.addFilter(record -> true);
    template.setFilter(record -> false);

    Assert.assertEquals(template.getFilters().length, 1);
  }

  public void testSetFiltersReplacesAll () {

    DefaultTemplate template = new DefaultTemplate();

    template.addFilter(record -> true);
    template.setFilters(new Filter[] {record -> false, record -> true});

    Assert.assertEquals(template.getFilters().length, 2);
  }

  public void testAddAndRemoveAppender () {

    DefaultTemplate template = new DefaultTemplate();
    Appender first = new CapturingAppender();
    Appender second = new CapturingAppender();

    template.addAppender(first);
    template.addAppender(second);
    Assert.assertEquals(template.getAppenders().length, 2);

    template.removeAppender(first);
    Assert.assertEquals(template.getAppenders().length, 1);
    Assert.assertSame(template.getAppenders()[0], second);
  }

  public void testAddAndRemoveEnhancer () {

    DefaultTemplate template = new DefaultTemplate();
    Enhancer first = record -> {
    };
    Enhancer second = record -> {
    };

    template.addEnhancer(first);
    template.addEnhancer(second);
    Assert.assertEquals(template.getEnhancers().length, 2);

    template.removeEnhancer(first);
    Assert.assertEquals(template.getEnhancers().length, 1);
    Assert.assertSame(template.getEnhancers()[0], second);
  }

  public void testSetLevelAndAutoFillAccessors () {

    DefaultTemplate template = new DefaultTemplate();

    template.setLevel(Level.ERROR);
    template.setAutoFillLoggerContext(true);

    Assert.assertEquals(template.getLevel(), Level.ERROR);
    Assert.assertTrue(template.isAutoFillLoggerContext());
  }

  public void testSetAppenderAndSetEnhancerConvenienceReplaceAll () {

    DefaultTemplate template = new DefaultTemplate();

    template.addAppender(new CapturingAppender());
    template.setAppender(new CapturingAppender());
    Assert.assertEquals(template.getAppenders().length, 1);

    template.addEnhancer(record -> {
    });
    template.setEnhancer(record -> {
    });
    Assert.assertEquals(template.getEnhancers().length, 1);
  }

  public void testRemoveNonexistentMembersAreNoOps () {

    DefaultTemplate template = new DefaultTemplate();

    template.removeFilter(record -> true);
    template.removeAppender(new CapturingAppender());
    template.removeEnhancer(record -> {
    });

    Assert.assertEquals(template.getFilters().length, 0);
    Assert.assertEquals(template.getAppenders().length, 0);
    Assert.assertEquals(template.getEnhancers().length, 0);
  }

  public void testApplyPushesLevelAndContextToLogger () {

    DefaultTemplate template = new DefaultTemplate(Level.WARN, true);
    Logger logger = new Logger("template.apply.all." + System.nanoTime());

    template.addFilter(record -> true);
    template.addAppender(new CapturingAppender());
    template.addEnhancer(record -> {
    });

    template.apply(logger);

    Assert.assertEquals(logger.getLevel(), Level.WARN);
    Assert.assertTrue(logger.getAutoFillLoggerContext());
  }

  public void testApplyChangeHandlesEachAspect () {

    DefaultTemplate template = new DefaultTemplate(Level.ERROR, true, new CapturingAppender());
    Logger logger = new Logger("template.apply.change." + System.nanoTime());

    template.addFilter(record -> true);
    template.addEnhancer(record -> {
    });

    for (Template.Change change : Template.Change.values()) {
      template.applyChange(change, logger);
    }

    Assert.assertEquals(logger.getLevel(), Level.ERROR);
    Assert.assertTrue(logger.getAutoFillLoggerContext());
  }
}
