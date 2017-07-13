/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
import java.util.Arrays;
import org.smallmind.nutsnbolts.util.Pair;

public class SuiteRunner implements Runnable {

  private AnnotationProcessor annotationProcessor;
  private TestThreadPool threadPool;
  private DependencyQueue<Suite, Class<?>> suiteDependencyQueue;
  private Dependency<Suite, Class<?>> suiteDependency;

  public SuiteRunner (Dependency<Suite, Class<?>> suiteDependency, DependencyQueue<Suite, Class<?>> suiteDependencyQueue, AnnotationProcessor annotationProcessor, TestThreadPool threadPool) {

    this.suiteDependency = suiteDependency;
    this.suiteDependencyQueue = suiteDependencyQueue;
    this.annotationProcessor = annotationProcessor;
    this.threadPool = threadPool;
  }

  @Override
  public void run () {

    AnnotationDictionary annotationDictionary = annotationProcessor.process(suiteDependency.getValue());
    DependencyAnalysis<Test, Method> testMethodAnalysis = new DependencyAnalysis<>(Test.class);
    DependencyQueue<Test, Method> testMethodDependencyQueue;
    Dependency<Test, Method> testMethodDependency;
    AnnotationMethodology<BeforeSuite> beforeSuiteMethodology;
    AnnotationMethodology<AfterSuite> afterSuiteMethodology;
    AnnotationMethodology<Test> testMethodology;
    Object instance;

    try {
      instance = suiteDependency.getValue().getConstructor().newInstance();
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new TestProcessingException("Test class(%s) must expose a no arg constructor", suiteDependency.getValue().getName());
    } catch (Exception exception) {
      throw new TestProcessingException(exception);
    }

    if ((beforeSuiteMethodology = annotationDictionary.getBeforeSuiteMethodology()) != null) {
      beforeSuiteMethodology.invoke(instance);
    }

    if ((testMethodology = annotationDictionary.getTestMethodology()) != null) {
      for (Pair<Method, Test> testPair : testMethodology) {
        if (testPair.getSecond().enabled()) {
          testMethodAnalysis.add(new Dependency<>(testPair.getFirst().getName(), testPair.getSecond(), testPair.getFirst(), testPair.getSecond().priority(), testPair.getSecond().dependsOn()));
        }
      }
    }

    testMethodDependencyQueue = testMethodAnalysis.calculate();
    while ((testMethodDependency = testMethodDependencyQueue.poll()) != null) {
      try {
        threadPool.execute(TestTier.TEST, new TestRunner(suiteDependency.getValue(), instance, testMethodDependency, testMethodDependencyQueue, annotationProcessor));
      } catch (Exception exception) {
//TODO: Test Failure
      }
    }

    if ((afterSuiteMethodology = annotationDictionary.getAfterSuiteMethodology()) != null) {
      afterSuiteMethodology.invoke(instance);
    }

    suiteDependencyQueue.complete(suiteDependency);
  }
}
