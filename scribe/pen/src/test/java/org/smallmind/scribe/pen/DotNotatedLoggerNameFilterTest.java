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

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DotNotatedLoggerNameFilterTest {

  public void testNoArgConstructorPassesThroughAtInfo ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter();

    Assert.assertEquals(filter.getPassThroughLevel(), Level.INFO);
    Assert.assertTrue(filter.willLog(new RecordFixture().setLevel(Level.INFO).setLoggerName("any.Name")));
    Assert.assertFalse(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("any.Name")));
  }

  public void testPassThroughLevelAccessorsRoundTrip ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setPassThroughLevel(Level.ERROR);

    Assert.assertEquals(filter.getPassThroughLevel(), Level.ERROR);
  }

  public void testConstructorWithInitialPatterns ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN, List.of("com.foo.*"));

    Assert.assertTrue(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("com.foo.Widget")));
    Assert.assertFalse(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("com.bar.Widget")));
  }

  public void testSetPatternsReplacesActiveSet ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setPatterns(List.of("com.foo.*"));

    Assert.assertTrue(filter.isClassNameOn("com.foo.Widget"));
    Assert.assertFalse(filter.isClassNameOn("com.bar.Widget"));
  }

  public void testMatchedClassNameIsCachedForReuse ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setDebugCategory("com.example.*", true);

    // The first call populates the cache; the second resolves through the cached-name fast path.
    Assert.assertTrue(filter.isClassNameOn("com.example.Widget"));
    Assert.assertTrue(filter.isClassNameOn("com.example.Widget"));
  }

  public void testAddingSameDebugCategoryTwiceIsIdempotent ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setDebugCategory("com.example.*", true);
    filter.setDebugCategory("com.example.*", true);

    Assert.assertTrue(filter.isClassNameOn("com.example.Widget"));
  }

  public void testRecordsAtOrAbovePassThroughLevelAlwaysLog ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    Assert.assertTrue(filter.willLog(new RecordFixture().setLevel(Level.ERROR).setLoggerName("any.unmatched.Name")));
  }

  public void testRecordsBelowPassThroughLevelWithUnmatchedNameAreBlocked ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    Assert.assertFalse(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("any.unmatched.Name")));
  }

  public void testMatchingDebugCategoryEnablesBelowThresholdRecords ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setDebugCategory("com.example.*", true);

    Assert.assertTrue(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("com.example.Widget")));
    Assert.assertFalse(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("org.elsewhere.Widget")));
  }

  public void testRemovingDebugCategoryEvictsCachedMatch ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setDebugCategory("com.example.*", true);
    // Prime the match cache for this class name.
    Assert.assertTrue(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("com.example.Widget")));

    filter.setDebugCategory("com.example.*", false);

    Assert.assertFalse(filter.willLog(new RecordFixture().setLevel(Level.DEBUG).setLoggerName("com.example.Widget")));
  }

  public void testNullClassNameIsNeverOn ()
    throws LoggerException {

    DotNotatedLoggerNameFilter filter = new DotNotatedLoggerNameFilter(Level.WARN);

    filter.setDebugCategory("com.example.*", true);

    Assert.assertFalse(filter.isClassNameOn(null));
  }
}
