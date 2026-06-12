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

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

/**
 * Test-only {@link TestReportListener} that records which Surefire callback was invoked and the entry
 * it carried, so the Sleuth-to-Surefire translation can be asserted without a live reporting pipeline.
 * The {@code ConsoleLogger} methods inherited via {@link TestReportListener} are inert.
 */
public class CapturingRunListener implements TestReportListener<TestOutputReportEntry> {

  private final List<TestOutputReportEntry> output = new ArrayList<>();
  private final List<String> calls = new ArrayList<>();
  private TestSetReportEntry lastSetEntry;
  private ReportEntry lastReportEntry;
  private String lastCall;
  private boolean executionSkippedByUser;

  public String getLastCall () {

    return lastCall;
  }

  public List<String> getCalls () {

    return calls;
  }

  public ReportEntry getLastReportEntry () {

    return lastReportEntry;
  }

  public TestSetReportEntry getLastSetEntry () {

    return lastSetEntry;
  }

  public boolean wasExecutionSkippedByUser () {

    return executionSkippedByUser;
  }

  public List<TestOutputReportEntry> getOutput () {

    return output;
  }

  private void note (String call) {

    lastCall = call;
    calls.add(call);
  }

  @Override
  public void testSetStarting (TestSetReportEntry reportEntry) {

    note("testSetStarting");
    lastSetEntry = reportEntry;
  }

  @Override
  public void testSetCompleted (TestSetReportEntry reportEntry) {

    note("testSetCompleted");
    lastSetEntry = reportEntry;
  }

  @Override
  public void testStarting (ReportEntry reportEntry) {

    note("testStarting");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testSucceeded (ReportEntry reportEntry) {

    note("testSucceeded");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testAssumptionFailure (ReportEntry reportEntry) {

    note("testAssumptionFailure");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testError (ReportEntry reportEntry) {

    note("testError");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testFailed (ReportEntry reportEntry) {

    note("testFailed");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testSkipped (ReportEntry reportEntry) {

    note("testSkipped");
    lastReportEntry = reportEntry;
  }

  @Override
  public void testExecutionSkippedByUser () {

    note("testExecutionSkippedByUser");
    executionSkippedByUser = true;
  }

  @Override
  public void writeTestOutput (TestOutputReportEntry entry) {

    output.add(entry);
  }

  @Override
  public boolean isDebugEnabled () {

    return false;
  }

  @Override
  public void debug (String message) {

  }

  @Override
  public boolean isInfoEnabled () {

    return false;
  }

  @Override
  public void info (String message) {

  }

  @Override
  public boolean isWarnEnabled () {

    return false;
  }

  @Override
  public void warning (String message) {

  }

  @Override
  public boolean isErrorEnabled () {

    return false;
  }

  @Override
  public void error (String message) {

  }

  @Override
  public void error (String message, Throwable throwable) {

  }

  @Override
  public void error (Throwable throwable) {

  }
}
