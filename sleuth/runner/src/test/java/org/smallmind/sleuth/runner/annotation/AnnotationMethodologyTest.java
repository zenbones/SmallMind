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
package org.smallmind.sleuth.runner.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.sleuth.runner.CapturingSleuthEventListener;
import org.smallmind.sleuth.runner.Culprit;
import org.smallmind.sleuth.runner.SleuthRunner;
import org.smallmind.sleuth.runner.event.SleuthEventType;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AnnotationMethodologyTest {

  private static SleuthRunner runnerListeningWith (CapturingSleuthEventListener listener) {

    SleuthRunner sleuthRunner = new SleuthRunner();

    sleuthRunner.addListener(listener);

    return sleuthRunner;
  }

  private static int count (AnnotationMethodology<BeforeTest> methodology) {

    int count = 0;

    for (Pair<Method, BeforeTest> ignored : methodology) {
      count++;
    }

    return count;
  }

  public void testOverridingSignatureIsDeduplicated ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();

    // Hierarchy order (base first) mirrors what MethodCensus produces; the subclass override carries
    // the same name and parameter types and must collapse to a single entry.
    methodology.add(Base.class.getMethod("setUp"), new BeforeTestLiteral());
    methodology.add(Derived.class.getMethod("setUp"), new BeforeTestLiteral());

    Assert.assertEquals(count(methodology), 1);
  }

  public void testInvokesRegisteredMethodsInOrderEmittingSuccessEvents ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();
    Recorder recorder = new Recorder();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    methodology.add(Recorder.class.getMethod("alpha"), new BeforeTestLiteral());
    methodology.add(Recorder.class.getMethod("beta"), new BeforeTestLiteral());

    Culprit culprit = methodology.invoke(runnerListeningWith(listener), null, Recorder.class, recorder);

    Assert.assertNull(culprit);
    Assert.assertEquals(recorder.getCalls(), List.of("alpha", "beta"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "alpha"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "beta"));
  }

  public void testExistingCulpritSkipsAllMethods ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();
    Recorder recorder = new Recorder();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();
    Culprit priorCulprit = new Culprit("org.example.Prior", "earlier", new RuntimeException("prior"));

    methodology.add(Recorder.class.getMethod("alpha"), new BeforeTestLiteral());

    Culprit returned = methodology.invoke(runnerListeningWith(listener), priorCulprit, Recorder.class, recorder);

    Assert.assertSame(returned, priorCulprit, "An existing culprit must pass through unchanged");
    Assert.assertTrue(recorder.getCalls().isEmpty(), "No method should run when a culprit is already present");
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "alpha"));
  }

  public void testThrowingMethodSetsCulpritAndSuppressesLaterMethods ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();
    Recorder recorder = new Recorder();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    methodology.add(Recorder.class.getMethod("boom"), new BeforeTestLiteral());
    methodology.add(Recorder.class.getMethod("alpha"), new BeforeTestLiteral());

    Culprit culprit = methodology.invoke(runnerListeningWith(listener), null, Recorder.class, recorder);

    Assert.assertNotNull(culprit, "A throwing method must produce a culprit");
    Assert.assertEquals(recorder.getCalls(), List.of("boom"), "Once a method throws, later methods are skipped");
    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "boom"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "alpha"));
  }

  public void testNonInvocationTargetFailureIsReportedAsError ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    methodology.add(Recorder.class.getMethod("alpha"), new BeforeTestLiteral());

    // Invoking against an instance that is not a Recorder makes reflective invoke throw an
    // IllegalArgumentException (not an InvocationTargetException), exercising the generic catch.
    Culprit culprit = methodology.invoke(runnerListeningWith(listener), null, Recorder.class, new Object());

    Assert.assertNotNull(culprit);
    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "alpha"));
  }

  public void testSameNameDifferentParametersAreNotDeduplicated ()
    throws NoSuchMethodException {

    AnnotationMethodology<BeforeTest> methodology = new AnnotationMethodology<>();

    methodology.add(Overloaded.class.getMethod("hook"), new BeforeTestLiteral());
    methodology.add(Overloaded.class.getMethod("hook", String.class), new BeforeTestLiteral());

    Assert.assertEquals(count(methodology), 2, "Overloads differing only in parameters are distinct keys");
  }

  public static class Overloaded {

    public void hook () {

    }

    public void hook (String argument) {

    }
  }

  public static class Recorder {

    private final List<String> calls = new ArrayList<>();

    public List<String> getCalls () {

      return calls;
    }

    public void alpha () {

      calls.add("alpha");
    }

    public void beta () {

      calls.add("beta");
    }

    public void boom () {

      calls.add("boom");

      throw new IllegalStateException("kaboom");
    }
  }

  public static class Base {

    public void setUp () {

    }
  }

  public static class Derived extends Base {

    @Override
    public void setUp () {

    }
  }
}
