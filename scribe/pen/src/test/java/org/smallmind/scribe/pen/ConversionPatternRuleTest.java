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

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link ConversionPatternRule} through its string constructor (the one the
 * {@link PatternFormatter} parser uses), pinning the genuinely tricky pure-string transforms —
 * width padding, truncation, dot-notation precision, message fallback, and escape resolution —
 * which are otherwise only reachable indirectly through formatting.
 */
@Test(groups = "unit")
public class ConversionPatternRuleTest {

  public void testRightPaddingFillsToWidth () {

    ConversionPatternRule rule = new ConversionPatternRule(null, "+", "6", null, null, null, "l", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setLevel(Level.INFO), null), "INFO  ");
  }

  public void testLeftPaddingFillsToWidth () {

    ConversionPatternRule rule = new ConversionPatternRule(null, "-", "6", null, null, null, "l", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setLevel(Level.INFO), null), "  INFO");
  }

  public void testValueLongerThanWidthIsTruncated () {

    ConversionPatternRule rule = new ConversionPatternRule(null, "+", "2", null, null, null, "l", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setLevel(Level.INFO), null), "IN");
  }

  public void testDotPrecisionKeepsTrailingSegments () {

    ConversionPatternRule rule = new ConversionPatternRule(null, null, null, "2", null, null, "n", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setLoggerName("com.example.app.Widget"), null), "app.Widget");
  }

  public void testAbsentPrecisionLeavesNameWhole () {

    ConversionPatternRule rule = new ConversionPatternRule(null, null, null, null, null, null, "n", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setLoggerName("com.example.Widget"), null), "com.example.Widget");
  }

  public void testMessageFallsBackToThrowableMessage () {

    ConversionPatternRule rule = new ConversionPatternRule(null, null, null, null, null, null, "m", null);

    Assert.assertEquals(rule.convert(new RecordFixture().setMessage(null).setThrown(new RuntimeException("boom")), null), "boom");
  }

  public void testMissingLoggerContextRendersNull () {

    ConversionPatternRule rule = new ConversionPatternRule(null, null, null, null, null, null, "C", null);

    Assert.assertNull(rule.convert(new RecordFixture().setLoggerContext(null), null));
  }

  public void testHeaderTabEscapeIsResolved () {

    ConversionPatternRule rule = new ConversionPatternRule("x\\ty", null, null, null, null, null, "l", null);

    Assert.assertEquals(rule.getHeader(), "x\ty");
  }

  public void testHeaderNewlineEscapeBecomesPlatformSeparator () {

    ConversionPatternRule rule = new ConversionPatternRule("a\\nb", null, null, null, null, null, "l", null);

    Assert.assertEquals(rule.getHeader(), "a" + System.lineSeparator() + "b");
  }

  public void testEpochMillisToken () {

    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "t", null).convert(new RecordFixture().setMillis(999L), null), "999");
  }

  public void testDateTokenIsRenderedThroughTheTimestamp () {

    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "d", null).convert(new RecordFixture().setMillis(0L), new NullTimestamp()), "");
  }

  public void testContextTokensRenderFromFilledContext () {

    RecordFixture record = new RecordFixture().setLoggerContext(new LoggerContextFixture(true, "com.example.Widget", "doWork", "Widget.java", 42, false));

    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "C", null).convert(record, null), "com.example.Widget");
    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "M", null).convert(record, null), "doWork");
    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "F", null).convert(record, null), "Widget.java");
    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "L", null).convert(record, null), "42");
    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "N", null).convert(record, null), "false");
  }

  public void testContextTokenSuppressedWhenContextNotFilled () {

    RecordFixture record = new RecordFixture().setLoggerContext(new LoggerContextFixture(false, "x", "y", "z", 1, false));

    Assert.assertNull(new ConversionPatternRule(null, null, null, null, null, null, "M", null).convert(record, null));
  }

  public void testStackTraceRendersCausedByChain () {

    Throwable inner = new IllegalStateException("inner cause");
    Throwable outer = new RuntimeException("outer failure", inner);

    String rendered = new ConversionPatternRule(null, null, null, null, "-", " ", "s", null).convert(new RecordFixture().setThrown(outer), null);

    Assert.assertTrue(rendered.contains("Exception in thread"));
    Assert.assertTrue(rendered.contains("outer failure"));
    Assert.assertTrue(rendered.contains("Caused by:"));
    Assert.assertTrue(rendered.contains("inner cause"));
  }

  public void testParameterPrecisionLimitsRenderedEntries () {

    Parameter[] parameters = new Parameter[] {new Parameter("a", "1"), new Parameter("b", "2"), new Parameter("c", "3")};

    String rendered = new ConversionPatternRule(null, null, null, "2", "-", ",", "p", null).convert(new RecordFixture().setParameters(parameters), null);

    Assert.assertEquals(rendered, "a=1,b=2");
  }

  public void testStripSlashesResolvesControlEscapes () {

    Assert.assertEquals(new ConversionPatternRule("a\\rb", null, null, null, null, null, "l", null).getHeader(), "a\rb");
    Assert.assertEquals(new ConversionPatternRule("a\\fb", null, null, null, null, null, "l", null).getHeader(), "a\fb");
  }

  public void testStripSlashesKeepsTrailingBackslashAndDropsUnknownEscape () {

    Assert.assertEquals(new ConversionPatternRule("x\\", null, null, null, null, null, "l", null).getHeader(), "x\\");
    Assert.assertEquals(new ConversionPatternRule("a\\qb", null, null, null, null, null, "l", null).getHeader(), "aqb");
  }

  public void testNonePaddingLeavesShortValueUnchanged () {

    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "l", null).convert(new RecordFixture().setLevel(Level.INFO), null), "INFO");
  }

  public void testThreadNameToken () {

    Assert.assertEquals(new ConversionPatternRule(null, null, null, null, null, null, "T", null).convert(new RecordFixture().setThreadName("worker-7"), null), "worker-7");
  }

  public void testHeaderAndFooterAccessors () {

    ConversionPatternRule rule = new ConversionPatternRule("HEAD", null, null, null, null, null, "l", "FOOT");

    Assert.assertEquals(rule.getHeader(), "HEAD");
    Assert.assertEquals(rule.getFooter(), "FOOT");
  }

  @Test(expectedExceptions = UnknownSwitchCaseException.class)
  public void testUnknownConversionCharacterThrows () {

    new ConversionPatternRule(null, null, null, null, null, null, "q", null).convert(new RecordFixture(), null);
  }
}
