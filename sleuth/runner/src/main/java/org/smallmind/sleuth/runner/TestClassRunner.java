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
import java.util.HashMap;

public class TestClassRunner implements Runnable {

  private TestThreadPool threadPool;
  private DependencyQueue<TestClass> testClassDependencyQueue;
  private Dependency<TestClass> testClassDependency;
  private Class<?> clazz;
  private Object instance;

  public TestClassRunner (Class<?> clazz, Object instance, Dependency<TestClass> testClassDependency, DependencyQueue<TestClass> testClassDependencyQueue, TestThreadPool threadPool) {

    this.clazz = clazz;
    this.instance = instance;
    this.testClassDependency = testClassDependency;
    this.testClassDependencyQueue = testClassDependencyQueue;
    this.threadPool = threadPool;
  }

  @Override
  public void run () {

    DependencyAnalysis<Test> testMethodAnalysis = new DependencyAnalysis<>(Test.class);
    HashMap<String, Method> methodMap = new HashMap<>();
    DependencyQueue<Test> testMethodDependencyQueue;
    Dependency<Test> testMethodDependency;

    for (Method method : clazz.getMethods()) {

      Test test;

      if ((test = method.getAnnotation(Test.class)) != null) {
        methodMap.put(method.getName(), method);
        testMethodAnalysis.add(new Dependency<>(method.getName(), test, test.priority(), test.dependsOn()));
      }

      if (method.getAnnotation(BeforeClass.class) != null) {
        try {
          method.invoke(instance);
        } catch (Exception exception) {
          throw new TestProcessingException(exception);
        }
      }
    }

    testMethodDependencyQueue = new DependencyQueue<>(testMethodAnalysis.calculate());
    while ((testMethodDependency = testMethodDependencyQueue.poll()) != null) {
      try {
        threadPool.execute(TestTier.METHOD, new TestMethodRunner(clazz, instance, methodMap.get(testMethodDependency.getName()), testMethodDependency, testMethodDependencyQueue));
      } catch (Exception exception) {
//TODO: Test Failure
      }
    }

    for (Method method : clazz.getMethods()) {
      if (method.getAnnotation(AfterClass.class) != null) {
        try {
          method.invoke(instance);
        } catch (Exception exception) {
          throw new TestProcessingException(exception);
        }
      }
    }

    testClassDependencyQueue.complete(testClassDependency);
  }
}
