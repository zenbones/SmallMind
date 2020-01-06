/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.sleuth.runner.event.ErrorSleuthEvent;
import org.smallmind.sleuth.runner.event.FailureSleuthEvent;
import org.smallmind.sleuth.runner.event.FatalSleuthEvent;
import org.smallmind.sleuth.runner.event.MootSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEventListener;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

public class SurefireSleuthEventListener implements SleuthEventListener {

  private RunListener runListener;
  private Throwable throwable;

  public SurefireSleuthEventListener (RunListener runListener) {

    this.runListener = runListener;
  }

  public Throwable getThrowable () {

    return throwable;
  }

  @Override
  public void handle (SleuthEvent event) {

    switch (event.getType()) {
      case START:
        runListener.testStarting(new SimpleReportEntry(event.getClassName(), event.getMethodName()));
        break;
      case SUCCESS:
        runListener.testSucceeded(new SimpleReportEntry(event.getClassName(), event.getMethodName(), (int)((SuccessSleuthEvent)event).getElapsed()));
        break;
      case FAILURE:
        runListener.testFailed(new SimpleReportEntry(event.getClassName(), event.getMethodName(), new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((FailureSleuthEvent)event).getThrowable()), (int)((FailureSleuthEvent)event).getElapsed()));
        break;
      case ERROR:
        runListener.testError(new SimpleReportEntry(event.getClassName(), event.getMethodName(), new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((ErrorSleuthEvent)event).getThrowable()), (int)((ErrorSleuthEvent)event).getElapsed()));
        break;
      case SKIPPED:
        runListener.testSkipped(new SimpleReportEntry(event.getClassName(), event.getMethodName(), ((SkippedSleuthEvent)event).getMessage()));
        break;
      case MOOT:
        runListener.testAssumptionFailure(new SimpleReportEntry(event.getClassName(), event.getMethodName(), new SleuthStackTraceWriter(event.getClassName(), event.getMethodName(), ((MootSleuthEvent)event).getThrowable()), (int)((MootSleuthEvent)event).getElapsed()));
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
