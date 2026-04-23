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
 * Assertion errors ({@link AssertionError}) are reported as
 * {@link org.smallmind.sleuth.runner.event.SleuthEventType#FAILURE}; all other exceptions are
 * reported as {@link org.smallmind.sleuth.runner.event.SleuthEventType#ERROR}. Either may trigger
 * run cancellation if the corresponding stop flag is set. {@link #complete()} is always called in
 * a {@code finally} block to ensure latches and semaphores are correctly released.
 *
 * @see SuiteRunner
 * @see SleuthRunner
 */
public class TestRunner implements TestController {

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
   * invoked via reflection; on success a {@link SuccessSleuthEvent} is fired. An {@link AssertionError}
   * fires a {@link FailureSleuthEvent}; any other exception fires an {@link ErrorSleuthEvent}. In
   * both error cases the culprit is set so after-test hooks see it. After-test methods run regardless
   * of test outcome. The final culprit is stored on the dependency node for downstream tests.
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
          sleuthRunner.fire(new SuccessSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds));
        } catch (InvocationTargetException invocationTargetException) {
          culprit = new Culprit(clazz.getName(), testMethodDependency.getValue().getName(), invocationTargetException.getCause());
          if (invocationTargetException.getCause() instanceof AssertionError) {
            if (stopOnFailure) {
              sleuthRunner.cancel();
            }
            sleuthRunner.fire(new FailureSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, invocationTargetException.getCause()));
          } else {
            if (stopOnError) {
              sleuthRunner.cancel();
            }
            sleuthRunner.fire(new ErrorSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, invocationTargetException.getCause()));
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
