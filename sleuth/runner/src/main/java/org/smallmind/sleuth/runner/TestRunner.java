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
 * Executes a single test method along with its per-test lifecycle hooks.
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
   * @param sleuthRunner              runner used for event dispatch and cancellation checks
   * @param testCompletedLatch        latch decremented when the test finishes
   * @param culprit                   prior failure that should cause this test to be skipped; may be {@code null}
   * @param clazz                     test class
   * @param instance                  instance of the test class
   * @param testMethodDependency      dependency metadata for the test method
   * @param testMethodDependencyQueue queue managing inter-test dependencies
   * @param annotationProcessor       processor translating annotations into executable metadata
   * @param threadPool                thread pool used to execute test tiers
   * @param stopOnError               whether unexpected errors halt further execution
   * @param stopOnFailure             whether assertion failures halt further execution
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
   * Executes before/after test hooks and the target test method, emitting Sleuth events for each stage.
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
   * Releases the semaphore slot, marks the dependency complete, and counts down the latch.
   */
  @Override
  public void complete () {

    threadPool.release(TestTier.TEST);
    testMethodDependencyQueue.complete(testMethodDependency);
    testCompletedLatch.countDown();
  }
}
