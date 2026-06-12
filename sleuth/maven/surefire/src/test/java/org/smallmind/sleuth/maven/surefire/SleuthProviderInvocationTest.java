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

import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.testng.Assert;
import org.testng.annotations.Test;

// Drives the full SleuthProvider through hand-rolled surefire-api doubles (dynamic proxies for the
// query interfaces, the concrete TestsToRun/RunResult for values). invoke() swaps System.out/err for
// ForwardingPrintStream instances, so every call is wrapped to restore the originals afterward. Sample
// suites use Sleuth's native annotations and are nested static classes named so the Surefire scan
// ignores them.
@Test(groups = "unit")
public class SleuthProviderInvocationTest {

  // Returns defaults for every method except those named in the returns map. Lets a few-method test
  // double stand in for the wide ProviderParameters/ScanResult/ReporterFactory interfaces.
  @SuppressWarnings("unchecked")
  private static <T> T stub (Class<T> iface, Map<String, Object> returns) {

    return (T)Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, (proxy, method, args) -> {
      if (returns.containsKey(method.getName())) {

        return returns.get(method.getName());
      }

      Class<?> returnType = method.getReturnType();

      if (returnType.equals(int.class)) {

        return 0;
      }
      if (returnType.equals(long.class)) {

        return 0L;
      }
      if (returnType.equals(boolean.class)) {

        return false;
      }

      return null;
    });
  }

  private static TestsToRun testsToRun (Class<?>... classes) {

    return new TestsToRun(new LinkedHashSet<>(Arrays.asList(classes)));
  }

  private static ProviderParameters parameters (TestsToRun scanned, CapturingRunListener listener, RunResult runResult, Map<String, String> providerProperties) {

    ScanResult scanResult = stub(ScanResult.class, Map.of("applyFilter", scanned));
    ReporterFactory reporterFactory = stub(ReporterFactory.class, Map.of("createTestReportListener", listener, "close", runResult));
    Map<String, Object> values = new HashMap<>();

    values.put("getScanResult", scanResult);
    values.put("getReporterFactory", reporterFactory);
    values.put("getTestClassLoader", SleuthProviderInvocationTest.class.getClassLoader());
    values.put("getProviderProperties", providerProperties);

    return stub(ProviderParameters.class, values);
  }

  private static RunResult invokeRestoringStreams (SleuthProvider provider, Object forkTestSet)
    throws Exception {

    PrintStream outSave = System.out;
    PrintStream errSave = System.err;

    try {

      return provider.invoke(forkTestSet);
    } finally {
      System.setOut(outSave);
      System.setErr(errSave);
    }
  }

  public void testGetSuitesDelegatesToScanResult () {

    TestsToRun suites = testsToRun(PassingProviderSuite.class);
    SleuthProvider provider = new SleuthProvider(parameters(suites, new CapturingRunListener(), new RunResult(0, 0, 0, 0), new HashMap<>()));

    Assert.assertSame(provider.getSuites(), suites);
  }

  public void testInvokeAfterGetSuitesRunsAndReturnsReporterResult ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    RunResult runResult = new RunResult(1, 0, 0, 0);
    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, runResult, new HashMap<>()));

    provider.getSuites();

    RunResult result = invokeRestoringStreams(provider, null);

    Assert.assertSame(result, runResult, "invoke returns the ReporterFactory.close() result");
    Assert.assertTrue(listener.getCalls().contains("testSetStarting"));
    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
    Assert.assertTrue(listener.getCalls().contains("testSetCompleted"));
  }

  public void testForkTestSetAsTestsToRunIsUsedDirectly ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    TestsToRun suites = testsToRun(PassingProviderSuite.class);
    SleuthProvider provider = new SleuthProvider(parameters(suites, listener, new RunResult(0, 0, 0, 0), new HashMap<>()));

    invokeRestoringStreams(provider, suites);

    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
  }

  public void testForkTestSetAsClassResolvesViaFromClass ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(), listener, new RunResult(0, 0, 0, 0), new HashMap<>()));

    invokeRestoringStreams(provider, PassingProviderSuite.class);

    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
  }

  public void testNullForkTestSetTriggersScan ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, new RunResult(0, 0, 0, 0), new HashMap<>()));

    invokeRestoringStreams(provider, null);

    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
  }

  public void testThreadCountAndTestFailureIgnoreProperties ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    Map<String, String> properties = new HashMap<>();

    properties.put("threadCount", "2");
    properties.put("testFailureIgnore", "true");

    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, new RunResult(0, 0, 0, 0), properties));

    invokeRestoringStreams(provider, null);

    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
  }

  public void testLowercaseThreadcountProperty ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    Map<String, String> properties = new HashMap<>();

    properties.put("threadcount", "3");

    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, new RunResult(0, 0, 0, 0), properties));

    invokeRestoringStreams(provider, null);

    Assert.assertTrue(listener.getCalls().contains("testSucceeded"));
  }

  public void testGroupsSystemPropertyOverridesProviderProperty ()
    throws Exception {

    String saved = System.getProperty("groups");

    System.setProperty("groups", "all");
    try {

      CapturingRunListener listener = new CapturingRunListener();
      Map<String, String> properties = new HashMap<>();

      // The provider property would otherwise filter the ungrouped suite out; the "all" system
      // property must take precedence and run everything.
      properties.put("groups", "nonexistent-group");

      SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, new RunResult(0, 0, 0, 0), properties));

      invokeRestoringStreams(provider, null);

      Assert.assertTrue(listener.getCalls().contains("testSucceeded"), "the 'all' system property should override the provider's group filter");
    } finally {
      if (saved == null) {
        System.clearProperty("groups");
      } else {
        System.setProperty("groups", saved);
      }
    }
  }

  public void testFatalEventIsRethrownAsTestSetFailedException ()
    throws Exception {

    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(FailingConstructorProviderSuite.class), new CapturingRunListener(), new RunResult(0, 0, 0, 0), new HashMap<>()));

    try {
      invokeRestoringStreams(provider, null);
      Assert.fail("A captured FatalSleuthEvent should surface as a TestSetFailedException");
    } catch (TestSetFailedException testSetFailedException) {
      // expected
    }
  }

  public void testMultipleSuitesAllRunAndBuildTheTestSetName ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    TestsToRun suites = testsToRun(PassingProviderSuite.class, SecondPassingProviderSuite.class);
    SleuthProvider provider = new SleuthProvider(parameters(suites, listener, new RunResult(0, 0, 0, 0), new HashMap<>()));

    invokeRestoringStreams(provider, suites);

    // Both suites run (covering the comma-separator branch of the test-set name builder).
    Assert.assertEquals(Collections.frequency(listener.getCalls(), "testSucceeded"), 2);
  }

  public void testCancelStopsExecution ()
    throws Exception {

    CapturingRunListener listener = new CapturingRunListener();
    SleuthProvider provider = new SleuthProvider(parameters(testsToRun(PassingProviderSuite.class), listener, new RunResult(0, 0, 0, 0), new HashMap<>()));

    provider.cancel();
    invokeRestoringStreams(provider, null);

    Assert.assertFalse(listener.getCalls().contains("testSucceeded"), "a cancelled provider must not run any test");
  }

  @Suite(groups = {})
  public static class PassingProviderSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void passes () {

    }
  }

  @Suite(groups = {})
  public static class SecondPassingProviderSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void alsoPasses () {

    }
  }

  public static class FailingConstructorProviderSuite {

    public FailingConstructorProviderSuite (String required) {

    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void neverRuns () {

    }
  }
}
