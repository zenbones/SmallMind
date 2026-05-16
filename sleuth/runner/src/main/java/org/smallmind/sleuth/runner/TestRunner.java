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
package org.smallmind.sleuth.runner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.smallmind.sleuth.runner.annotation.AfterTest;
import org.smallmind.sleuth.runner.annotation.AnnotationDictionary;
import org.smallmind.sleuth.runner.annotation.AnnotationMethodology;
import org.smallmind.sleuth.runner.annotation.AnnotationProcessor;
import org.smallmind.sleuth.runner.annotation.BeforeTest;
import org.smallmind.sleuth.runner.annotation.Test;
import org.smallmind.sleuth.runner.event.ErrorSleuthEvent;
import org.smallmind.sleuth.runner.event.FailureSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.StartSleuthEvent;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

/**
 * Executes a single test method along with its per-test lifecycle hooks on the {@link TestTier#TEST} thread.
 * <p>
 * The runner runs before-test hooks, invokes the target test method, and runs after-test hooks,
 * emitting Sleuth events at each stage. If a non-null culprit is present at the start (inherited
 * from a suite-level failure or from the dependency queue), the test body is skipped and a
 * {@link SkippedSleuthEvent} is emitted instead.
 * <p>
 * If the {@code expectedExceptions} list contains at least one {@link Exception} subtype and the
 * test method returns normally, an {@link ExpectedExceptionNotThrownException} is raised and the
 * test is reported as {@link org.smallmind.sleuth.runner.event.SleuthEventType#FAILURE}.
 * When the test method throws, the exception is categorized against the {@code expectedExceptions}
 * list (non-{@link Exception} entries are ignored). A throw matching a listed {@link Exception}
 * type produces {@link org.smallmind.sleuth.runner.event.SleuthEventType#SUCCESS}. A throw that
 * fails to match any listed {@link Exception} type, or an {@link AssertionError}, produces
 * {@link org.smallmind.sleuth.runner.event.SleuthEventType#FAILURE}. All other uncategorized
 * exceptions produce {@link org.smallmind.sleuth.runner.event.SleuthEventType#ERROR}.
 * Failures and errors may trigger run cancellation if the corresponding stop flag is set.
 * {@link #complete()} is always called in a {@code finally} block to ensure latches and
 * semaphores are correctly released.
 *
 * @see SuiteRunner
 * @see SleuthRunner
 */
public class TestRunner implements TestController {

  private enum EXCEPTIONAL {IGNORED, EXPECTED, UNEXPECTED}

  private final SleuthRunner sleuthRunner;
  private final AnnotationProcessor annotationProcessor;
  private final SleuthThreadPool threadPool;
  private final DependencyQueue<Test, Method> testMethodDependencyQueue;
  private final Dependency<Test, Method> testMethodDependency;
  private final CountDownLatch testCompletedLatch;
  private final Class<?> clazz;
  private final Object instance;
  private final boolean stopOnError;
  private final boolean stopOnFailure;
  private Culprit culprit;

  /**
   * Constructs a runner for one test method.
   *
   * @param sleuthRunner              central runner for event dispatch and cancellation; must not be {@code null}
   * @param testCompletedLatch        latch decremented once this test finishes; must not be {@code null}
   * @param culprit                   suite-level culprit that causes this test to be skipped; {@code null} if none
   * @param clazz                     test class containing the method; must not be {@code null}
   * @param instance                  instance of the test class on which methods are invoked; must not be {@code null}
   * @param testMethodDependency      dependency node for this test method; must not be {@code null}
   * @param testMethodDependencyQueue queue used to mark this test complete when it finishes; must not be {@code null}
   * @param annotationProcessor       processor to resolve per-test lifecycle annotations; must not be {@code null}
   * @param threadPool                pool used to release the test-tier semaphore; must not be {@code null}
   * @param stopOnError               {@code true} to cancel the run on the first unexpected error
   * @param stopOnFailure             {@code true} to cancel the run on the first assertion failure
   */
  public TestRunner (SleuthRunner sleuthRunner, CountDownLatch testCompletedLatch, Culprit culprit, Class<?> clazz, Object instance, Dependency<Test, Method> testMethodDependency, DependencyQueue<Test, Method> testMethodDependencyQueue, AnnotationProcessor annotationProcessor, SleuthThreadPool threadPool, boolean stopOnError, boolean stopOnFailure) {

    this.sleuthRunner = sleuthRunner;
    this.testCompletedLatch = testCompletedLatch;
    this.culprit = (culprit == null) ? testMethodDependency.getCulprit() : culprit;
    this.clazz = clazz;
    this.instance = instance;
    this.testMethodDependency = testMethodDependency;
    this.testMethodDependencyQueue = testMethodDependencyQueue;
    this.annotationProcessor = annotationProcessor;
    this.threadPool = threadPool;
    this.stopOnError = stopOnError;
    this.stopOnFailure = stopOnFailure;
  }

  /**
   * Runs before-test hooks, the test method, and after-test hooks, emitting events throughout.
   * <p>
   * The test identifier is updated so output can be attributed to this test. Before-test methods
   * are invoked first; if any produce a culprit the test body is skipped. The test method is then
   * invoked via reflection. If it returns normally and {@link #expectsException()} is true, a
   * {@link FailureSleuthEvent} is fired for the missing throw; otherwise a {@link SuccessSleuthEvent}
   * is fired. If it throws, {@link #categorizeExpectedException} determines the outcome: a matching
   * listed {@link Exception} type fires a {@link SuccessSleuthEvent}; a non-matching listed
   * {@link Exception} type or an {@link AssertionError} fires a {@link FailureSleuthEvent}; all
   * other exceptions fire an {@link ErrorSleuthEvent}. In failure and error cases the culprit is
   * set so after-test hooks see it. After-test methods run regardless of test outcome. The final
   * culprit is stored on the dependency node for downstream tests.
   * <p>
   * {@link #complete()} is always called in a {@code finally} block.
   */
  @Override
  public void run () {

    TestIdentifier.updateIdentifier(clazz.getName(), testMethodDependency.getValue().getName());

    try {

      AnnotationDictionary annotationDictionary = annotationProcessor.process(clazz);
      AnnotationMethodology<BeforeTest> beforeTestMethodology;
      AnnotationMethodology<AfterTest> afterTestMethodology;

      if ((beforeTestMethodology = annotationDictionary.getBeforeTestMethodology()) != null) {
        culprit = beforeTestMethodology.invoke(sleuthRunner, culprit, clazz, instance);
      }

      sleuthRunner.fire(new StartSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName()));
      if (culprit != null) {
        sleuthRunner.fire(new SkippedSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), 0, "Skipped due to prior error[" + culprit + "]"));
      } else {

        long startMilliseconds = System.currentTimeMillis();

        try {
          testMethodDependency.getValue().invoke(instance);
          if (expectsException()) {

            ExpectedExceptionNotThrownException expectedExceptionNotThrownException = new ExpectedExceptionNotThrownException(Arrays.toString(testMethodDependency.getExpectedExceptions()));

            culprit = new Culprit(clazz.getName(), testMethodDependency.getValue().getName(), expectedExceptionNotThrownException);

            if (stopOnFailure) {
              sleuthRunner.cancel();
            }

            sleuthRunner.fire(new FailureSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, expectedExceptionNotThrownException));
          } else {
            sleuthRunner.fire(new SuccessSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds));
          }
        } catch (InvocationTargetException invocationTargetException) {

          Throwable targetException = invocationTargetException.getCause();
          EXCEPTIONAL exceptional;

          if (EXCEPTIONAL.EXPECTED.equals(exceptional = categorizeExpectedException(targetException))) {
            sleuthRunner.fire(new SuccessSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds));
          } else {

            culprit = new Culprit(clazz.getName(), testMethodDependency.getValue().getName(), targetException);

            if (EXCEPTIONAL.UNEXPECTED.equals(exceptional) || targetException instanceof AssertionError) {
              if (stopOnFailure) {
                sleuthRunner.cancel();
              }
              sleuthRunner.fire(new FailureSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, targetException));
            } else {
              if (stopOnError) {
                sleuthRunner.cancel();
              }
              sleuthRunner.fire(new ErrorSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, targetException));
            }
          }
        } catch (Exception exception) {
          if (stopOnError) {
            sleuthRunner.cancel();
          }
          culprit = new Culprit(clazz.getName(), testMethodDependency.getValue().getName(), exception);
          sleuthRunner.fire(new ErrorSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, exception));
        }
      }

      if ((afterTestMethodology = annotationDictionary.getAfterTestMethodology()) != null) {
        culprit = afterTestMethodology.invoke(sleuthRunner, culprit, clazz, instance);
      }

      testMethodDependency.setCulprit(culprit);
    } finally {

      complete();
    }
  }

  private boolean expectsException () {

    if ((testMethodDependency.getExpectedExceptions() == null) || (testMethodDependency.getExpectedExceptions().length == 0)) {

      return false;
    } else {
      for (Class<?> expectedException : testMethodDependency.getExpectedExceptions()) {
        if (Exception.class.isAssignableFrom(expectedException)) {

          return true;
        }
      }

      return false;
    }
  }

  /**
   * Categorizes a thrown exception against the node's {@code expectedExceptions} list.
   * <p>
   * Returns {@link EXCEPTIONAL#IGNORED} when the list is empty or {@code null}, meaning no
   * expected-exception contract is in effect and the caller applies normal exception handling.
   * <p>
   * For each entry in the list: entries that are not subtypes of {@link Exception} are silently
   * ignored and do not affect enforcement. For entries that are subtypes of {@link Exception}: if
   * the thrown throwable is an instance of the entry, returns {@link EXCEPTIONAL#EXPECTED}
   * immediately. If the entry does not match, the check is considered <em>enforced</em>.
   * After the loop, if any real {@link Exception} entry was seen but none matched, returns
   * {@link EXCEPTIONAL#UNEXPECTED}, treated as a test failure. If no {@link Exception} entry
   * was present, returns {@link EXCEPTIONAL#IGNORED} and normal exception handling applies.
   *
   * @param actuallyThrown the exception thrown by the test method; must not be {@code null}
   * @return the categorization of the thrown exception relative to the expected-exception contract
   */
  private EXCEPTIONAL categorizeExpectedException (Throwable actuallyThrown) {

    if ((testMethodDependency.getExpectedExceptions() == null) || (testMethodDependency.getExpectedExceptions().length == 0)) {

      return EXCEPTIONAL.IGNORED;
    } else {

      boolean enforced = false;

      for (Class<?> expectedException : testMethodDependency.getExpectedExceptions()) {
        if (Exception.class.isAssignableFrom(expectedException)) {
          if (expectedException.isAssignableFrom(actuallyThrown.getClass())) {

            return EXCEPTIONAL.EXPECTED;
          } else {
            enforced = true;
          }
        }
      }

      return enforced ? EXCEPTIONAL.UNEXPECTED : EXCEPTIONAL.IGNORED;
    }
  }

  /**
   * Releases the test-tier semaphore permit, marks this test complete in the queue, and
   * decrements the suite's test completion latch.
   * <p>
   * Always called from the {@code finally} block of {@link #run()}.
   */
  @Override
  public void complete () {

    threadPool.release(TestTier.TEST);
    testMethodDependencyQueue.complete(testMethodDependency);
    testCompletedLatch.countDown();
  }
}
