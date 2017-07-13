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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.smallmind.sleuth.runner.annotation.AfterSuite;
import org.smallmind.sleuth.runner.annotation.AfterTest;
import org.smallmind.sleuth.runner.annotation.AnnotationDictionary;
import org.smallmind.sleuth.runner.annotation.AnnotationMethodology;
import org.smallmind.sleuth.runner.annotation.AnnotationProcessor;
import org.smallmind.sleuth.runner.annotation.BeforeSuite;
import org.smallmind.sleuth.runner.annotation.BeforeTest;
import org.smallmind.sleuth.runner.annotation.Test;
import org.smallmind.sleuth.runner.event.FailureSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.StartSleuthEvent;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

public class TestRunner implements Runnable {

  private SleuthRunner sleuthRunner;
  private AnnotationProcessor annotationProcessor;
  private DependencyQueue<Test, Method> testMethodDependencyQueue;
  private Dependency<Test, Method> testMethodDependency;
  private Culprit culprit;
  private Class<?> clazz;
  private Object instance;

  public TestRunner (SleuthRunner sleuthRunner, Culprit culprit, Class<?> clazz, Object instance, Dependency<Test, Method> testMethodDependency, DependencyQueue<Test, Method> testMethodDependencyQueue, AnnotationProcessor annotationProcessor) {

    this.sleuthRunner = sleuthRunner;
    this.culprit = culprit;
    this.clazz = clazz;
    this.instance = instance;
    this.testMethodDependency = testMethodDependency;
    this.testMethodDependencyQueue = testMethodDependencyQueue;
    this.annotationProcessor = annotationProcessor;
  }

  @Override
  public void run () {

    try {

      AnnotationDictionary annotationDictionary = annotationProcessor.process(clazz);
      AnnotationMethodology<BeforeTest> beforeTestMethodology;
      AnnotationMethodology<AfterTest> afterTestMethodology;

      if ((beforeTestMethodology = annotationDictionary.getBeforeTestMethodology()) != null) {
        beforeTestMethodology.invoke(sleuthRunner, culprit, clazz, instance);
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
          sleuthRunner.fire(new FailureSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, invocationTargetException.getCause()));
        } catch (Exception exception) {
          culprit = new Culprit(clazz.getName(), testMethodDependency.getValue().getName(), exception);
          sleuthRunner.fire(new FailureSleuthEvent(clazz.getName(), testMethodDependency.getValue().getName(), System.currentTimeMillis() - startMilliseconds, exception));
        }
      }

      if ((afterTestMethodology = annotationDictionary.getAfterTestMethodology()) != null) {
        afterTestMethodology.invoke(sleuthRunner, culprit, clazz, instance);
      }
    } finally {
      testMethodDependencyQueue.complete(testMethodDependency);
    }
  }
}
