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

import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.smallmind.sleuth.runner.TestIdentifier;

/**
 * Bridges raw test output from the Sleuth runner into Surefire's reporting API.
 * <p>
 * Accepts {@link OutputReportEntry} objects from {@link ForwardingPrintStream} and wraps each
 * one in a {@link TestOutputReportEntry} that also carries the current {@link RunMode} and the
 * numeric test identifier obtained from {@link TestIdentifier#getTestIdentifier()}. This allows
 * Surefire to attribute every stdout/stderr line to the specific test that was running on the
 * current thread at the time the output was written.
 *
 * @see ForwardingPrintStream
 * @see TestIdentifier
 */
public class SleuthOutputReceiver implements TestOutputReceiver<OutputReportEntry> {

  private final TestReportListener<TestOutputReportEntry> reportListener;
  private final RunMode runMode;

  /**
   * Constructs a receiver that wraps and forwards output entries to the given Surefire listener.
   *
   * @param reportListener target Surefire listener that processes {@link TestOutputReportEntry} objects; must not be {@code null}
   * @param runMode        execution mode of the current run (e.g., {@link RunMode#NORMAL_RUN}); must not be {@code null}
   */
  public SleuthOutputReceiver (TestReportListener<TestOutputReportEntry> reportListener, RunMode runMode) {

    this.reportListener = reportListener;
    this.runMode = runMode;
  }

  /**
   * Wraps the raw output entry with the active run mode and test identifier, then forwards it
   * to the Surefire report listener.
   *
   * @param reportEntry raw output entry emitted by the Sleuth runner; must not be {@code null}
   */
  @Override
  public void writeTestOutput (OutputReportEntry reportEntry) {

    reportListener.writeTestOutput(new TestOutputReportEntry(reportEntry, runMode, TestIdentifier.getTestIdentifier()));
  }
}
