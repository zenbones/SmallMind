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

import java.util.Arrays;
import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.smallmind.nutsnbolts.util.AnsiColor;
import org.smallmind.sleuth.runner.SleuthRunner;

import static java.lang.System.setErr;
import static java.lang.System.setOut;

/**
 * Surefire {@link org.apache.maven.surefire.api.provider.SurefireProvider} implementation that
 * integrates the Sleuth test runner into the Maven Surefire plugin.
 * <p>
 * This class is the entry point for Maven-driven test execution. It is responsible for:
 * <ol>
 *   <li>Scanning the classpath for test suite classes via {@link #getSuites()}.</li>
 *   <li>Redirecting {@code System.out} and {@code System.err} to {@link ForwardingPrintStream}
 *       instances so all test output is routed through the Surefire reporting pipeline.</li>
 *   <li>Reading provider properties ({@code groups}, {@code threadCount}, {@code testFailureIgnore})
 *       and the {@code groups} system property to configure the run.</li>
 *   <li>Registering a {@link SurefireSleuthEventListener} that translates Sleuth events into
 *       Surefire {@link org.apache.maven.surefire.api.report.RunListener} calls.</li>
 *   <li>Delegating execution to {@link SleuthRunner#execute} and collecting the {@link RunResult}.</li>
 *   <li>Rethrowing any fatal throwable captured by the listener as a {@link TestSetFailedException}.</li>
 * </ol>
 * The {@code groups} property accepts a comma-separated list of group names. The sentinel value
 * {@code all} causes all groups to be included (equivalent to passing no group filter).
 *
 * @see SurefireSleuthEventListener
 * @see SleuthRunner
 */
public class SleuthProvider extends AbstractProvider {

  private final ProviderParameters providerParameters;
  private final SleuthRunner sleuthRunner = new SleuthRunner();
  private TestsToRun testsToRun;

  /**
   * Constructs the provider with the Maven-supplied execution parameters.
   *
   * @param providerParameters Surefire parameters including classpath scan results, classloaders,
   *                           reporter factories, and plugin configuration; must not be {@code null}
   */
  public SleuthProvider (ProviderParameters providerParameters) {

    this.providerParameters = providerParameters;
  }

  /**
   * Scans the test classpath for suite classes recognized by Sleuth and caches the result.
   * <p>
   * The result is cached in {@code testsToRun} so that {@link #invoke(Object)} can reuse it
   * without re-scanning when called in the same VM.
   *
   * @return iterable of test suite classes to execute; never {@code null}
   */
  @Override
  public Iterable<Class<?>> getSuites () {

    testsToRun = providerParameters.getScanResult().applyFilter(null, providerParameters.getTestClassLoader());

    return testsToRun;
  }

  /**
   * Requests cancellation of the currently executing suite set by delegating to
   * {@link SleuthRunner#cancel()}.
   */
  @Override
  public void cancel () {

    sleuthRunner.cancel();
  }

  /**
   * Executes all discovered suites and returns the aggregated {@link RunResult}.
   * <p>
   * If {@code testsToRun} was not populated by a prior call to {@link #getSuites()}, it is
   * resolved from the {@code forkTestSet} argument or by re-scanning the classpath. Provider
   * properties are read in this priority order:
   * <ol>
   *   <li>{@code groups} system property (overrides provider property)</li>
   *   <li>{@code groups} provider property</li>
   *   <li>{@code threadCount} / {@code threadcount} provider property (default: unbounded)</li>
   *   <li>{@code testFailureIgnore} provider property (default: stop on error and failure)</li>
   * </ol>
   *
   * @param forkTestSet when running in a forked VM, either a {@link TestsToRun} or a {@link Class}
   *                    supplied by the forking process; may be {@code null}
   * @return aggregated run result including pass, failure, error, and skip counts; never {@code null}
   * @throws TestSetFailedException if a {@link org.smallmind.sleuth.runner.event.FatalSleuthEvent}
   *                                was captured during execution
   * @throws ReporterException      if the reporter factory cannot create or close the listener
   */
  @Override
  public RunResult invoke (Object forkTestSet)
    throws TestSetFailedException, ReporterException {

    ReporterFactory reporterFactory = providerParameters.getReporterFactory();
    TestReportListener<TestOutputReportEntry> reportListener = reporterFactory.createTestReportListener();
    RunResult runResult;

    try {

      SleuthOutputReceiver sleuthOutputReceiver = new SleuthOutputReceiver(reportListener, RunMode.NORMAL_RUN);
      SurefireSleuthEventListener sleuthEventListener;
      StringBuilder testNameBuilder;
      String[] groups;
      boolean stopOnError = true;
      boolean stopOnFailure = true;
      long startMilliseconds;
      int threadCount = 0;
      int testIndex = 0;

      setOut(new ForwardingPrintStream(sleuthOutputReceiver, true));
      setErr(new ForwardingPrintStream(sleuthOutputReceiver, false));

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

      if (providerParameters.getProviderProperties().get("testFailureIgnore") != null) {
        stopOnError = !Boolean.parseBoolean(providerParameters.getProviderProperties().get("testFailureIgnore"));
        stopOnFailure = !Boolean.parseBoolean(providerParameters.getProviderProperties().get("testFailureIgnore"));
      }

      testNameBuilder = new StringBuilder("[");
      for (Class<?> testClass : testsToRun) {
        if (testIndex++ > 0) {
          testNameBuilder.append(',');
        }
        testNameBuilder.append(testClass.getSimpleName());
      }
      testNameBuilder.append(']');

      sleuthRunner.addListener(sleuthEventListener = new SurefireSleuthEventListener(reportListener, RunMode.NORMAL_RUN));
      startMilliseconds = System.currentTimeMillis();

      System.out.println(AnsiColor.YELLOW.getCode() + "Sleuth test set starting with thread count(" + threadCount + ") on groups " + (((groups == null) || (groups.length == 0)) ? "all" : Arrays.toString(groups)) + " in " + testNameBuilder + "..." + AnsiColor.DEFAULT.getCode());
      reportListener.testSetStarting(new SimpleReportEntry(RunMode.NORMAL_RUN, 0L, "sleuth_test"/*testNameBuilder.toString()*/, "Sleuth Tests", "Test Assay", "Name Text", "super message 0"));

      sleuthRunner.execute(((groups != null) && (groups.length == 0)) ? null : groups, (threadCount <= 0) ? Integer.MAX_VALUE : threadCount, stopOnError, stopOnFailure, testsToRun);

      System.out.println(AnsiColor.YELLOW.getCode() + "Sleuth test set completed in " + (System.currentTimeMillis() - startMilliseconds) + "ms" + AnsiColor.DEFAULT.getCode());
      reportListener.testSetCompleted(new SimpleReportEntry(RunMode.NORMAL_RUN, 0L, "sleuth_test"/*testNameBuilder.toString()*/, "Sleuth Tests", "Test Assay", "nameText", (int)(System.currentTimeMillis() - startMilliseconds)));

      if (sleuthEventListener.getThrowable() != null) {
        throw new TestSetFailedException(sleuthEventListener.getThrowable());
      }
    } finally {
      runResult = reporterFactory.close();
    }

    return runResult;
  }

  /**
   * Parses a comma-separated groups string into an array of group names.
   * <p>
   * Returns {@code null} when the input is absent or blank. Returns an empty array when any
   * element equals {@code "all"}, which serves as a sentinel meaning "include all groups". Otherwise
   * returns the individual group names in input order.
   *
   * @param groupsParameter raw comma-separated groups string; may be {@code null} or empty
   * @return {@code null} when no value is provided; empty array for the {@code all} sentinel;
   * otherwise an array of individual group names
   */
  private String[] parseGroups (String groupsParameter) {

    if ((groupsParameter != null) && (!groupsParameter.isEmpty())) {

      String[] groups;
      String[] parameterElements = groupsParameter.split(",", -1);
      int index = 0;

      groups = new String[parameterElements.length];
      for (String parameterElement : parameterElements) {
        if ("all".equals(parameterElement)) {

          return new String[0];
        } else {
          groups[index++] = parameterElement;
        }
      }

      return groups;
    }

    return null;
  }
}
