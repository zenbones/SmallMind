/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.sleuth.runner.annotation.AfterSuite;
import org.smallmind.sleuth.runner.annotation.AnnotationDictionary;
import org.smallmind.sleuth.runner.annotation.AnnotationMethodology;
import org.smallmind.sleuth.runner.annotation.AnnotationProcessor;
import org.smallmind.sleuth.runner.annotation.BeforeSuite;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.smallmind.sleuth.runner.annotation.Test;
import org.smallmind.sleuth.runner.event.FatalSleuthEvent;

public class SuiteRunner implements TestController {

  private final SleuthRunner sleuthRunner;
  private final AnnotationProcessor annotationProcessor;
  private final SleuthThreadPool threadPool;
  private final DependencyQueue<Suite, Class<?>> suiteDependencyQueue;
  private final Dependency<Suite, Class<?>> suiteDependency;
  private final CountDownLatch suiteCompletedLatch;
  private final boolean stopOnError;
  private final boolean stopOnFailure;

  public SuiteRunner (SleuthRunner sleuthRunner, CountDownLatch suiteCompletedLatch, Dependency<Suite, Class<?>> suiteDependency, DependencyQueue<Suite, Class<?>> suiteDependencyQueue, AnnotationProcessor annotationProcessor, SleuthThreadPool threadPool, boolean stopOnError, boolean stopOnFailure) {

    this.sleuthRunner = sleuthRunner;
    this.suiteCompletedLatch = suiteCompletedLatch;
    this.suiteDependency = suiteDependency;
    this.suiteDependencyQueue = suiteDependencyQueue;
    this.annotationProcessor = annotationProcessor;
    this.threadPool = threadPool;
    this.stopOnError = stopOnError;
    this.stopOnFailure = stopOnFailure;
  }

  @Override
  public void run () {

    long startMilliseconds = System.currentTimeMillis();

    try {

      AnnotationDictionary annotationDictionary = annotationProcessor.process(suiteDependency.getValue());
      DependencyAnalysis<Test, Method> testMethodAnalysis = new DependencyAnalysis<>(Test.class);
      DependencyQueue<Test, Method> testMethodDependencyQueue;
      Dependency<Test, Method> testMethodDependency;
      AnnotationMethodology<BeforeSuite> beforeSuiteMethodology;
      AnnotationMethodology<AfterSuite> afterSuiteMethodology;
      AnnotationMethodology<Test> testMethodology;
      CountDownLatch testCompletedLatch;
      Culprit culprit = suiteDependency.getCulprit();
      Object instance;

      try {
        instance = suiteDependency.getValue().getConstructor().newInstance();
      } catch (NoSuchMethodException noSuchMethodException) {
        throw new TestProcessingException("Test class(%s) must expose a no arg constructor", suiteDependency.getValue().getName());
      } catch (Exception exception) {
        throw new TestProcessingException(exception);
      }

      if ((beforeSuiteMethodology = annotationDictionary.getBeforeSuiteMethodology()) != null) {
        culprit = beforeSuiteMethodology.invoke(sleuthRunner, culprit, suiteDependency.getValue(), instance);
      }

      if ((testMethodology = annotationDictionary.getTestMethodology()) != null) {
        for (Pair<Method, Test> testPair : testMethodology) {
          if (testPair.getSecond().enabled()) {
            testMethodAnalysis.add(new Dependency<>(testPair.getFirst().getName(), testPair.getSecond(), testPair.getFirst(), testPair.getSecond().priority(), testPair.getSecond().executeAfter(), testPair.getSecond().dependsOn()));
          }
        }
      }

      testMethodDependencyQueue = testMethodAnalysis.calculate();
      testCompletedLatch = new CountDownLatch(testMethodDependencyQueue.size());
      while (sleuthRunner.isRunning() && ((testMethodDependency = testMethodDependencyQueue.poll()) != null)) {
        try {
          threadPool.execute(TestTier.TEST, new TestRunner(sleuthRunner, testCompletedLatch, culprit, suiteDependency.getValue(), instance, testMethodDependency, testMethodDependencyQueue, annotationProcessor, threadPool, stopOnError, stopOnFailure));
        } catch (InterruptedException interruptedException) {
          culprit = new Culprit(SuiteRunner.class.getName(), "run", interruptedException);
          Thread.currentThread().interrupt();
        }
      }

      if (sleuthRunner.isRunning()) {
        testCompletedLatch.await();
      }

      if ((afterSuiteMethodology = annotationDictionary.getAfterSuiteMethodology()) != null) {
        culprit = afterSuiteMethodology.invoke(sleuthRunner, culprit, suiteDependency.getValue(), instance);
      }

      suiteDependency.setCulprit(culprit);
    } catch (Exception exception) {
      if (stopOnError) {
        sleuthRunner.cancel();
      }
      sleuthRunner.fire(new FatalSleuthEvent(SuiteRunner.class.getName(), "run", System.currentTimeMillis() - startMilliseconds, exception));
    } finally {
      complete();
    }
  }

  @Override
  public void complete () {

    threadPool.release(TestTier.SUITE);
    suiteDependencyQueue.complete(suiteDependency);
    suiteCompletedLatch.countDown();
  }
}
