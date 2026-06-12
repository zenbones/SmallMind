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
package org.smallmind.sleuth.runner;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CulpritTest {

  public void testReportsFirstFrameMatchingTheOriginatingClass () {

    RuntimeException throwable = new RuntimeException("boom");

    throwable.setStackTrace(new StackTraceElement[] {
      new StackTraceElement("org.other.Helper", "help", "Helper.java", 10),
      new StackTraceElement("com.example.Foo", "doIt", "Foo.java", 42),
      new StackTraceElement("com.example.Foo", "deeper", "Foo.java", 88)
    });

    Culprit culprit = new Culprit("com.example.Foo", "doIt", throwable);

    // The first frame belonging to the originating class wins, so line 42 (not 88).
    Assert.assertEquals(culprit.toString(), "com.example.Foo.doIt:42 RuntimeException boom");
  }

  public void testWalksCauseChainToLocateOriginatingFrame () {

    RuntimeException cause = new RuntimeException("root cause");

    cause.setStackTrace(new StackTraceElement[] {
      new StackTraceElement("com.example.Foo", "doIt", "Foo.java", 7)
    });

    IllegalStateException wrapper = new IllegalStateException("wrapper", cause);

    wrapper.setStackTrace(new StackTraceElement[] {
      new StackTraceElement("org.other.Wrapper", "wrap", "Wrapper.java", 99)
    });

    Culprit culprit = new Culprit("com.example.Foo", "doIt", wrapper);

    // The matching frame lives in the cause, so the cause's type and message are reported.
    Assert.assertEquals(culprit.toString(), "com.example.Foo.doIt:7 RuntimeException root cause");
  }

  public void testFallsBackToSuppliedCoordinatesWhenNoFrameMatches () {

    RuntimeException throwable = new RuntimeException("no match here");

    throwable.setStackTrace(new StackTraceElement[] {
      new StackTraceElement("org.other.X", "y", "X.java", 1)
    });

    Culprit culprit = new Culprit("com.example.Missing", "gone", throwable);

    // No frame matches, so the constructor-supplied class/method are used with line -1.
    Assert.assertEquals(culprit.toString(), "com.example.Missing.gone:-1 RuntimeException no match here");
  }
}
