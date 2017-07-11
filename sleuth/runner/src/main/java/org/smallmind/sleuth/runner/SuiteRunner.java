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
import java.util.Map;

public class SuiteRunner implements Runnable {

  private TestThreadPool threadPool;
  private HashMap<Class<?>, TestClass> classMap;
  private DependencyQueue<Suite> suiteDependencyQueue;
  private Dependency<Suite> suiteDependency;

  public SuiteRunner (HashMap<Class<?>, TestClass> classMap, Dependency<Suite> suiteDependency, DependencyQueue<Suite> suiteDependencyQueue, TestThreadPool threadPool) {

    this.classMap = classMap;
    this.suiteDependency = suiteDependency;
    this.suiteDependencyQueue = suiteDependencyQueue;
    this.threadPool = threadPool;
  }

  @Override
  public void run () {

    DependencyAnalysis<TestClass> testClassAnalysis = new DependencyAnalysis<>(TestClass.class);
    HashMap<String, TestInABox> instanceMap = new HashMap<>();
    DependencyQueue<TestClass> testClassDependencyQueue;
    Dependency<TestClass> testClassDependency;

    for (Map.Entry<Class<?>, TestClass> classEntry : classMap.entrySet()) {

      Object instance;

      try {
        instanceMap.put(classEntry.getKey().getName(), new TestInABox(classEntry.getKey(), instance = classEntry.getKey().getConstructor().newInstance()));
      } catch (NoSuchMethodException noSuchMethodException) {
        throw new TestProcessingException("Test class(%s) must expose a no arg constructor", classEntry.getKey().getName());
      } catch (Exception exception) {
        throw new TestProcessingException(exception);
      }
      testClassAnalysis.add(new Dependency<>(classEntry.getKey().getName(), classEntry.getValue(), classEntry.getValue().priority(), classEntry.getValue().dependsOn()));

      for (Method method : classEntry.getKey().getMethods()) {
        if (method.getAnnotation(BeforeSuite.class) != null) {
          try {
            method.invoke(instance);
          } catch (Exception exception) {
            throw new TestProcessingException(exception);
          }
        }
      }
    }

    testClassDependencyQueue = new DependencyQueue<>(testClassAnalysis.calculate());
    while ((testClassDependency = testClassDependencyQueue.poll()) != null) {

      TestInABox testInABox = instanceMap.get(testClassDependency.getName());

      try {
        threadPool.execute(TestTier.CLASS, new TestClassRunner(testInABox.getClazz(), testInABox.getInstance(), testClassDependency, testClassDependencyQueue, threadPool));
      } catch (Exception exception) {
//TODO: Test Failure
      }
    }

    for (Class<?> clazz : classMap.keySet()) {
      for (Method method : clazz.getMethods()) {
        if (method.getAnnotation(AfterSuite.class) != null) {
          try {
            method.invoke(instanceMap.get(clazz.getName()));
          } catch (Exception exception) {
            throw new TestProcessingException(exception);
          }
        }
      }
    }

    suiteDependencyQueue.complete(suiteDependency);
  }

  private class TestInABox {

    private Class<?> clazz;
    private Object instance;

    private TestInABox (Class<?> clazz, Object instance) {

      this.clazz = clazz;
      this.instance = instance;
    }

    private Class<?> getClazz () {

      return clazz;
    }

    private Object getInstance () {

      return instance;
    }
  }
}
