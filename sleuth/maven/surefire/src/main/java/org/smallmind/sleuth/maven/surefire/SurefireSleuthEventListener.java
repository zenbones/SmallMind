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

import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.AnsiColor;
import org.smallmind.sleuth.runner.TestIdentifier;
import org.smallmind.sleuth.runner.event.ErrorSleuthEvent;
import org.smallmind.sleuth.runner.event.FailureSleuthEvent;
import org.smallmind.sleuth.runner.event.FatalSleuthEvent;
import org.smallmind.sleuth.runner.event.MootSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEventListener;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

/**
 * Adapts Sleuth runner events to Surefire {@link RunListener} callbacks.
 * <p>
 * The listener converts each Sleuth event into the appropriate Surefire report entry and tracks fatal
 * throwables so they can be rethrown to Maven after execution completes.
 */
public class SurefireSleuthEventListener implements SleuthEventListener {

  private final RunListener runListener;
  private final RunMode runMode;
  private Throwable throwable;

  /**
   * Creates a new listener bound to the provided Surefire listener and run mode.
   *
   * @param runListener destination for translated events
   * @param runMode     execution mode used in report entries
   */
  public SurefireSleuthEventListener (RunListener runListener, RunMode runMode) {

    this.runListener = runListener;
    this.runMode = runMode;
  }

  /**
   * @return any fatal throwable captured during execution, otherwise {@code null}
   */
  public Throwable getThrowable () {

    return throwable;
  }

  /**
   * Translates a Sleuth event into a Surefire notification, recording fatal errors for later propagation.
   *
   * @param event event emitted by the Sleuth runner
   */
  @Override
  public void handle (SleuthEvent event) {

    System.out.println("[" + AnsiColor.YELLOW.getCode() + "SUREFIRE" + AnsiColor.DEFAULT.getCode() + "] " + event);

    switch (event.getType()) {
      case SETUP:
        runListener.testSetStarting(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText"));
        break;
      case START:
        runListener.testStarting(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText"));
        break;
      case SUCCESS:
        runListener.testSucceeded(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText", (int)((SuccessSleuthEvent)event).getElapsed()));
        break;
      case FAILURE:
        runListener.testFailed(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText", new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((FailureSleuthEvent)event).getThrowable()), (int)((FailureSleuthEvent)event).getElapsed()));
        break;
      case ERROR:
        runListener.testError(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText", new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((ErrorSleuthEvent)event).getThrowable()), (int)((ErrorSleuthEvent)event).getElapsed()));
        break;
      case SKIPPED:
        runListener.testSkipped(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), ((SkippedSleuthEvent)event).getMessage()));
        break;
      case CANCELLED:
        runListener.testSetCompleted(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "Tests have been cancelled"));
        break;
      case MOOT:
        runListener.testAssumptionFailure(new SimpleReportEntry(runMode, TestIdentifier.getTestIdentifier(), event.getClassName(), "source text", event.getMethodName(), "nameText", new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((MootSleuthEvent)event).getThrowable()), (int)((MootSleuthEvent)event).getElapsed()));
        break;
      case FATAL:
        throwable = ((FatalSleuthEvent)event).getThrowable();
        runListener.testExecutionSkippedByUser();
        break;
      default:
        throw new UnknownSwitchCaseException(event.getType().name());
    }
  }
}
