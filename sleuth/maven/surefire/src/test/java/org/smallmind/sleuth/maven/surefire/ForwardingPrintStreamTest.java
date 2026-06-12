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
package org.smallmind.sleuth.maven.surefire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ForwardingPrintStreamTest {

  public void testWriteSingleByteForwardsAsStdOutEntry () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.write((int)'A');

    Assert.assertEquals(receiver.getEntries().size(), 1);
    Assert.assertEquals(receiver.last().getLog(), "A");
    Assert.assertTrue(receiver.last().isStdOut());
  }

  public void testWriteByteArrayForwardsWholeArray () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.write("hello".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(receiver.last().getLog(), "hello");
  }

  public void testWriteByteArraySliceForwardsOnlyTheSlice () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.write("hello".getBytes(StandardCharsets.UTF_8), 1, 3);

    Assert.assertEquals(receiver.last().getLog(), "ell");
  }

  public void testPrintlnNullBecomesNullLiteralWithSeparator () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.println((String)null);

    Assert.assertEquals(receiver.last().getLog(), "null" + System.lineSeparator());
  }

  public void testPrintlnAppendsSeparator () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.println("line");

    Assert.assertEquals(receiver.last().getLog(), "line" + System.lineSeparator());
  }

  public void testStdErrFlagIsPropagated () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, false);

    stream.write((int)'x');

    Assert.assertFalse(receiver.last().isStdOut());
  }

  public void testCloseAndFlushAreNoOps () {

    CapturingOutputReceiver receiver = new CapturingOutputReceiver();
    ForwardingPrintStream stream = new ForwardingPrintStream(receiver, true);

    stream.close();
    stream.flush();

    Assert.assertTrue(receiver.getEntries().isEmpty(), "close()/flush() must not emit any output");
  }

  public void testReportEntryConstructorsExposeTheirFields () {

    SleuthReportEntry twoArg = new SleuthReportEntry("message", true);

    Assert.assertEquals(twoArg.getLog(), "message");
    Assert.assertTrue(twoArg.isStdOut());
    Assert.assertFalse(twoArg.isNewLine());

    SleuthReportEntry threeArg = new SleuthReportEntry("message", false, true);

    Assert.assertFalse(threeArg.isStdOut());
    Assert.assertTrue(threeArg.isNewLine());
  }

  private static class CapturingOutputReceiver implements TestOutputReceiver<OutputReportEntry> {

    private final List<OutputReportEntry> entries = new ArrayList<>();

    private List<OutputReportEntry> getEntries () {

      return entries;
    }

    private OutputReportEntry last () {

      return entries.get(entries.size() - 1);
    }

    @Override
    public void writeTestOutput (OutputReportEntry entry) {

      entries.add(entry);
    }
  }
}
