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
package org.smallmind.sleuth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class TestRunner {

  public static void execute (int maxThreads, String[] groups, Class... testClasses)
    throws InterruptedException {

    TestThreadPool threadPool = new TestThreadPool((maxThreads <= 0) ? Integer.MAX_VALUE : maxThreads);
    Suite defaultSuite = new SuiteLiteral();
    Tests defaultTests = new TestsLiteral();
    DependencyAnalysis<Suite> suiteAnalysis = new DependencyAnalysis<>(Suite.class);
    HashMap<Suite, HashSet<Class<?>>> suiteMap = new HashMap<>();
    LinkedList<Dependency<Suite>> suiteDependencyList;

    for (Class<?> testClass : testClasses) {

      Tests tests;

      if ((tests = testClass.getAnnotation(Tests.class)) == null) {
        tests = defaultTests;
      }
      if (tests.active() && ((groups == null) || inGroups(tests.group(), groups))) {

        Suite suite;
        HashSet<Class<?>> classSet;

        if ((suite = testClass.getAnnotation(Suite.class)) != null) {
          if (suite.name().trim().isEmpty()) {
            throw new TestDefinitionException("Suite defined on class(%s) must have a non-empty name", testClass.getName());
          } else if (defaultSuite.name().equals(suite.name())) {
            throw new TestDefinitionException("Suite defined on class(%s) uses the reserved name '%s'", testClass.getName(), defaultSuite.name());
          }
          suiteAnalysis.add(new Dependency<>(suite.name(), suite, suite.dependsOn()));
        } else {
          suite = defaultSuite;
        }

        if ((classSet = suiteMap.get(suite)) == null) {
          suiteMap.put(suite, classSet = new HashSet<>());
        }
        classSet.add(testClass);
      }
    }

    suiteDependencyList = suiteAnalysis.calculate();
    if (suiteMap.containsKey(defaultSuite)) {
      suiteDependencyList.addFirst(new Dependency<>(defaultSuite.name(), defaultSuite, defaultSuite.dependsOn()));
    }

    while (!suiteDependencyList.isEmpty()) {

      DependencyQueue<Suite> suiteDependencyQueue = new DependencyQueue<>(suiteDependencyList);
      Dependency<Suite> suiteDependency;

      while ((suiteDependency = suiteDependencyQueue.poll()) != null) {
        threadPool.execute(new SuiteRunner(suiteMap.get(suiteDependency.getValue()), suiteDependency, suiteDependencyQueue));
      }
    }
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
