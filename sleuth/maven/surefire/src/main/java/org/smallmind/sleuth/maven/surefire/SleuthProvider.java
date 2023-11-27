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

import java.util.Arrays;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;
import org.smallmind.sleuth.runner.SleuthRunner;
import org.smallmind.sleuth.runner.SleuthThreadPool;

public class SleuthProvider extends AbstractProvider {

  private final ProviderParameters providerParameters;
  private final SleuthRunner sleuthRunner = new SleuthRunner();
  private TestsToRun testsToRun;

  public SleuthProvider (ProviderParameters providerParameters) {

    this.providerParameters = providerParameters;
  }

  @Override
  public Iterable<Class<?>> getSuites () {

    testsToRun = providerParameters.getScanResult().applyFilter(null, providerParameters.getTestClassLoader());

    return testsToRun;
  }

  @Override
  public void cancel () {

    sleuthRunner.cancel();
  }

  @Override
  public RunResult invoke (Object forkTestSet)
    throws TestSetFailedException, ReporterException {

    ReporterFactory reporterFactory = providerParameters.getReporterFactory();
    RunResult runResult;
    RunListener runListener = reporterFactory.createReporter();
    SurefireSleuthEventListener sleuthEventListener;
    StringBuilder testNameBuilder;
    String[] groups;
    long startMilliseconds;
    int threadCount = 0;
    int testIndex = 0;

    System.setOut(new ForwardingPrintStream((ConsoleOutputReceiver)runListener, true));
    System.setErr(new ForwardingPrintStream((ConsoleOutputReceiver)runListener, false));

    if ((groups = parseGroups(System.getProperty("groups"))) == null) {
      groups = parseGroups(providerParameters.getProviderProperties().get("groups"));
    }

    if (testsToRun == null) {
      if (forkTestSet instanceof TestsToRun) {
        testsToRun = (TestsToRun)forkTestSet;
      } else if (forkTestSet instanceof Class) {
        testsToRun = TestsToRun.fromClass((Class<?>)forkTestSet);
      } else {
        testsToRun = providerParameters.getScanResult().applyFilter(null, providerParameters.getTestClassLoader());
      }
    }

    if (providerParameters.getProviderProperties().get("threadCount") != null) {
      threadCount = Integer.parseInt(providerParameters.getProviderProperties().get("threadCount"));
    } else if (providerParameters.getProviderProperties().get("threadcount") != null) {
      threadCount = Integer.parseInt(providerParameters.getProviderProperties().get("threadcount"));
    }

    testNameBuilder = new StringBuilder("[");
    for (Class<?> testClass : testsToRun) {
      if (testIndex++ > 0) {
        testNameBuilder.append(',');
      }
      testNameBuilder.append(testClass.getSimpleName());
    }
    testNameBuilder.append(']');

    sleuthRunner.addListener(sleuthEventListener = new SurefireSleuthEventListener(runListener));
    startMilliseconds = System.currentTimeMillis();

    System.out.println("Sleuth test set starting with thread count(" + threadCount + ") on groups " + Arrays.toString(groups) + " in " + testNameBuilder + "...");
    runListener.testSetStarting(new SimpleReportEntry("Sleuth Tests", "Test Assay", "test set starting"));

    sleuthRunner.execute(groups, new SleuthThreadPool((threadCount <= 0) ? Integer.MAX_VALUE : threadCount), testsToRun);

    System.out.println("Sleuth test set completed in " + (System.currentTimeMillis() - startMilliseconds) + "ms");
    runListener.testSetCompleted(new SimpleReportEntry("Sleuth Tests", "Test Assay", (int)(System.currentTimeMillis() - startMilliseconds)));

    runResult = reporterFactory.close();

    if (sleuthEventListener.getThrowable() != null) {
      throw new TestSetFailedException(sleuthEventListener.getThrowable());
    }

    return runResult;
  }

  private String[] parseGroups (String groupsParameter) {

    if ((groupsParameter != null) && (!groupsParameter.isEmpty())) {

      String[] groups;
      String[] parameterElements = groupsParameter.split(",", -1);
      int index = 0;

      groups = new String[parameterElements.length];
      for (String parameterElement : parameterElements) {
        groups[index++] = parameterElement;
      }

      return groups;
    }

    return null;
  }
}
