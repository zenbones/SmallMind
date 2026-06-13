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
package org.smallmind.web.json.scaffold.fault;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the {@link Fault#toString} rendering paths directly over hand-built {@link Fault} graphs:
 * the {@code context}-prefixed header, the {@code unknown} fallback when no throwable type is set, a
 * multi-level cause chain rendered with {@code Caused by:} headers, and the trailing
 * {@code ... N more} collapse produced when a cause's stack shares trailing frames with its parent.
 */
@Test(groups = "unit")
public class FaultPrintTest {

  private FaultElement frame (String type, String method, int line) {

    return new FaultElement(type, method, "Source.java", line);
  }

  public void testToStringWithContextPrefix () {

    Fault fault = new Fault(new FaultElement("Caller", "call"), "boom");
    fault.setThrowableType("java.lang.RuntimeException");

    String rendered = fault.toString();

    Assert.assertTrue(rendered.startsWith("Error in process at "), rendered);
    Assert.assertTrue(rendered.contains("java.lang.RuntimeException: boom"), rendered);
  }

  public void testToStringUnknownThrowableType () {

    Fault fault = new Fault("messageOnly");

    Assert.assertTrue(fault.toString().contains("unknown: messageOnly"), fault.toString());
  }

  public void testToStringRendersStackFrames () {

    Fault fault = new Fault("framed");
    fault.setThrowableType("java.lang.IllegalStateException");
    fault.setElements(new FaultElement[] {frame("A", "one", 1), frame("B", "two", 2)});

    String rendered = fault.toString();

    Assert.assertTrue(rendered.contains("   at A.one(Source.java:1)"), rendered);
    Assert.assertTrue(rendered.contains("   at B.two(Source.java:2)"), rendered);
  }

  public void testCauseChainCollapsesRepeatedFrames () {

    FaultElement shared = frame("Shared", "common", 99);

    Fault outer = new Fault("outer");
    outer.setThrowableType("java.lang.RuntimeException");
    outer.setElements(new FaultElement[] {frame("Outer", "run", 10), shared});

    Fault inner = new Fault("inner");
    inner.setThrowableType("java.lang.IllegalArgumentException");
    inner.setElements(new FaultElement[] {frame("Inner", "fail", 20), shared});
    outer.setCause(inner);

    String rendered = outer.toString();

    Assert.assertTrue(rendered.contains("Caused by: java.lang.IllegalArgumentException: inner"), rendered);
    Assert.assertTrue(rendered.contains("   at Inner.fail(Source.java:20)"), rendered);
    Assert.assertTrue(rendered.contains("... 1 more"), rendered);
  }
}
