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
 * Integration-level coverage of the {@link PatternFormatter} parser plus conversion pipeline: token
 * rendering, literal/escape handling, padding and precision, the message-to-throwable fallback, the
 * suppress-header-and-footer-when-the-field-is-absent contract, and the always-appended trailing line
 * separator. Patterns avoiding {@code %d} keep assertions independent of the JVM time zone.
 */
@Test(groups = "unit")
public class PatternFormatterTest {

  private static final String NEW_LINE = System.lineSeparator();

  public void testLevelTokenAndTrailingLineSeparator () {

    String formatted = new PatternFormatter("%l").format(new RecordFixture().setLevel(Level.WARN));

    Assert.assertEquals(formatted, "WARN" + NEW_LINE);
  }

  public void testLiteralTextWithEscapedPercent () {

    String formatted = new PatternFormatter("at 100%% done: %m").format(new RecordFixture().setMessage("ok"));

    Assert.assertEquals(formatted, "at 100% done: ok" + NEW_LINE);
  }

  public void testEpochMillisToken () {

    String formatted = new PatternFormatter("%t").format(new RecordFixture().setMillis(1234L));

    Assert.assertEquals(formatted, "1234" + NEW_LINE);
  }

  public void testLoggerNamePrecisionTrimsToTrailingSegments () {

    String formatted = new PatternFormatter("%.2n").format(new RecordFixture().setLoggerName("com.example.app.Widget"));

    Assert.assertEquals(formatted, "app.Widget" + NEW_LINE);
  }

  public void testRightPaddedLevel () {

    String formatted = new PatternFormatter("%+6l").format(new RecordFixture().setLevel(Level.INFO));

    Assert.assertEquals(formatted, "INFO  " + NEW_LINE);
  }

  public void testLeftPaddedLevel () {

    String formatted = new PatternFormatter("%-6l").format(new RecordFixture().setLevel(Level.INFO));

    Assert.assertEquals(formatted, "  INFO" + NEW_LINE);
  }

  public void testMessageFallsBackToThrowableMessage () {

    String formatted = new PatternFormatter("%m").format(new RecordFixture().setMessage(null).setThrown(new RuntimeException("boom")));

    Assert.assertEquals(formatted, "boom" + NEW_LINE);
  }

  public void testInlineParameterRendering () {

    Parameter[] parameters = new Parameter[] {new Parameter("requestId", "abc"), new Parameter("tenant", "acme")};

    String formatted = new PatternFormatter("%!-,!p").format(new RecordFixture().setParameters(parameters));

    Assert.assertEquals(formatted, "requestId=abc,tenant=acme" + NEW_LINE);
  }

  public void testFooterSuppressedWhenFieldIsAbsent () {

    // No throwable means the %s conversion yields null, which must also suppress its footer text.
    String formatted = new PatternFormatter("%!-,!sTRAILER}").format(new RecordFixture().setThrown(null));

    Assert.assertEquals(formatted, NEW_LINE);
    Assert.assertFalse(formatted.contains("TRAILER"));
  }

  public void testFooterEmittedWhenFieldIsPresent () {

    String formatted = new PatternFormatter("%!-,!sTRAILER}").format(new RecordFixture().setThrown(new RuntimeException("boom")));

    Assert.assertTrue(formatted.contains("Exception in thread"));
    Assert.assertTrue(formatted.contains("TRAILER"));
  }
}
