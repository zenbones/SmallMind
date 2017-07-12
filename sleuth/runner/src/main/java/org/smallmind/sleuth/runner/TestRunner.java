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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class TestRunner {

  public static void execute (int maxThreads, String[] groups, Class<?>... classes)
    throws InterruptedException {

    execute(maxThreads, groups, Arrays.asList(classes));
  }

  public static void execute (int maxThreads, String[] groups, Iterable<Class<?>> classIterable)
    throws InterruptedException {

    if (classIterable != null) {

      TestThreadPool threadPool = new TestThreadPool((maxThreads <= 0) ? Integer.MAX_VALUE : maxThreads);
      DependencyAnalysis<Suite> suiteAnalysis = new DependencyAnalysis<>(Suite.class);
      HashMap<Suite, HashMap<Class<?>, TestClass>> suiteMap = new HashMap<>();
      LinkedList<Dependency<Suite>> suiteDependencyList;
      DependencyQueue<Suite> suiteDependencyQueue;
      Dependency<Suite> suiteDependency;
      Suite defaultSuite = new SuiteLiteral();
      TestClass defaultTestClass = new TestClassLiteral();

      for (Class<?> clazz : classIterable) {

        if (isSleuthTest(clazz)) {

          TestClass testClass;

          if ((testClass = clazz.getAnnotation(TestClass.class)) == null) {
            testClass = defaultTestClass;
          }
          if (testClass.active() && ((groups == null) || inGroups(testClass.group(), groups))) {

            Suite suite;
            HashMap<Class<?>, TestClass> classMap;

            if ((suite = clazz.getAnnotation(Suite.class)) != null) {
              if (suite.name().trim().isEmpty()) {
                throw new TestDefinitionException("Suite defined on class(%s) must have a non-empty name", clazz.getName());
              } else if (defaultSuite.name().equals(suite.name())) {
                throw new TestDefinitionException("Suite defined on class(%s) uses the reserved name '%s'", clazz.getName(), defaultSuite.name());
              }
              suiteAnalysis.add(new Dependency<>(suite.name(), suite, suite.priority(), suite.dependsOn()));
            } else {
              suite = defaultSuite;
            }

            if ((classMap = suiteMap.get(suite)) == null) {
              suiteMap.put(suite, classMap = new HashMap<>());
            }
            classMap.put(clazz, testClass);
          }
        }
      }

      suiteDependencyList = suiteAnalysis.calculate();
      if (suiteMap.containsKey(defaultSuite)) {
        suiteDependencyList.addFirst(new Dependency<>(defaultSuite.name(), defaultSuite, defaultSuite.priority(), defaultSuite.dependsOn()));
      }

      suiteDependencyQueue = new DependencyQueue<>(suiteDependencyList);
      while ((suiteDependency = suiteDependencyQueue.poll()) != null) {
        try {
          threadPool.execute(TestTier.SUITE, new SuiteRunner(suiteMap.get(suiteDependency.getValue()), suiteDependency, suiteDependencyQueue, threadPool));
        } catch (Exception exception) {
//TODO: Test Failure
        }
      }

      threadPool.await();
    }
  }

  private static boolean isSleuthTest (Class<?> clazz) {

    for (Annotation annotation : clazz.getAnnotations()) {
      if ((annotation instanceof TestClass) || (annotation instanceof Suite)) {

        return true;
      }
    }
    for (Method method : clazz.getMethods()) {
      for (Annotation annotation : method.getAnnotations()) {
        if ((annotation instanceof Test) || (annotation instanceof BeforeTest) || (annotation instanceof AfterTest) || (annotation instanceof BeforeClass) || (annotation instanceof AfterClass) || (annotation instanceof BeforeSuite) || (annotation instanceof AfterSuite)) {

          return true;
        }
      }
    }

    return false;
  }

  private static boolean inGroups (String name, String[] names) {

    if ((name != null) && (names != null)) {
      for (String possibility : names) {
        if (name.equals(possibility)) {

          return true;
        }
      }
    }

    return false;
  }
}
