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
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEventListener;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

/**
 * Translates Sleuth runner events into Surefire {@link RunListener} notifications.
 * <p>
 * Registered with {@link org.smallmind.sleuth.runner.SleuthRunner} by {@link SleuthProvider}
 * before the test run starts. For every {@link SleuthEvent} received it:
 * <ol>
 *   <li>Prints the event to {@code System.out} with ANSI coloring for console visibility.</li>
 *   <li>Constructs a {@link SimpleReportEntry} embedding the current {@link RunMode} and the
 *       active numeric test identifier from {@link TestIdentifier#getTestIdentifier()}.</li>
 *   <li>Calls the appropriate {@link RunListener} method for the event type:
 *     <ul>
 *       <li>{@code START} → {@link RunListener#testStarting}</li>
 *       <li>{@code SUCCESS} → {@link RunListener#testSucceeded}</li>
 *       <li>{@code FAILURE} → {@link RunListener#testFailed} with a {@link SleuthStackTraceWriter}</li>
 *       <li>{@code ERROR} → {@link RunListener#testError} with a {@link SleuthStackTraceWriter}</li>
 *       <li>{@code SKIPPED} → {@link RunListener#testSkipped}</li>
 *       <li>{@code CANCELLED} → {@link RunListener#testSetCompleted}</li>
 *       <li>{@code FATAL} → captures the throwable and calls {@link RunListener#testExecutionSkippedByUser}</li>
 *     </ul>
 *   </li>
 * </ol>
 * The throwable captured from a {@code FATAL} event is retrievable via {@link #getThrowable()} so
 * {@link SleuthProvider} can rethrow it as a {@code TestSetFailedException} after the run ends.
 *
 * @see SleuthProvider
 * @see SleuthStackTraceWriter
 * @see org.smallmind.sleuth.runner.event.SleuthEventListener
 */
public class SurefireSleuthEventListener implements SleuthEventListener {

  private final RunListener runListener;
  private final RunMode runMode;
  private Throwable throwable;

  /**
   * Constructs a listener bound to the given Surefire listener and run mode.
   *
   * @param runListener destination for translated Surefire notifications; must not be {@code null}
   * @param runMode     execution mode embedded in every report entry (e.g., {@link RunMode#NORMAL_RUN}); must not be {@code null}
   */
  public SurefireSleuthEventListener (RunListener runListener, RunMode runMode) {

    this.runListener = runListener;
    this.runMode = runMode;
  }

  /**
   * Returns the throwable captured from the most recent {@code FATAL} event, or {@code null}
   * if no fatal event has been received.
   * <p>
   * {@link SleuthProvider} checks this after the run completes and rethrows the throwable as a
   * {@code TestSetFailedException} to fail the Maven build.
   *
   * @return the fatal throwable, or {@code null}
   */
  public Throwable getThrowable () {

    return throwable;
  }

  /**
   * Receives a Sleuth event, logs it to the console, and translates it into the corresponding
   * Surefire {@link RunListener} callback.
   * <p>
   * An {@link UnknownSwitchCaseException} is thrown if an unrecognized event type is encountered,
   * signaling that the switch statement needs updating.
   *
   * @param event the Sleuth event emitted by the runner; must not be {@code null}
   * @throws UnknownSwitchCaseException if {@code event.getType()} does not match any known {@link org.smallmind.sleuth.runner.event.SleuthEventType}
   */
  @Override
  public void handle (SleuthEvent event) {

    System.out.println("[" + AnsiColor.YELLOW.getCode() + "SUREFIRE" + AnsiColor.DEFAULT.getCode() + "] " + event);

    switch (event.getType()) {
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
      case FATAL:
        throwable = ((FatalSleuthEvent)event).getThrowable();
        runListener.testExecutionSkippedByUser();
        break;
      default:
        throw new UnknownSwitchCaseException(event.getType().name());
    }
  }
}
