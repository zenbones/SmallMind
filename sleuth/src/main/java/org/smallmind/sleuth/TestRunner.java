package org.smallmind.sleuth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class TestRunner {

  public void execute (Class... testClasses) {

    Suite defaultSuite = new SuiteLiteral();
    DependencyAnalysis<Suite> suiteAnalysis = new DependencyAnalysis<>(Suite.class);
    HashMap<Suite, HashSet<Class<?>>> suiteMap = new HashMap<>();
    LinkedList<Suite> suiteList;

    for (Class<?> testClass : testClasses) {

      Suite suite;
      HashSet<Class<?>> classSet;

      if ((suite = testClass.getAnnotation(Suite.class)) != null) {
        if (suite.name().trim().isEmpty()) {
          throw new TestDefinitionException("Suite defined on class(%s) must have a non-empty name", testClass.getName());
        } else if ("default".equals(suite.name())) {
          throw new TestDefinitionException("Suite defined on class(%s) uses the reserved name 'default'", testClass.getName());
        }
        suiteAnalysis.add(new Dependency<>(suite.name(), suite), suite.dependsOn());
      } else {
        suite = defaultSuite;
      }

      if ((classSet = suiteMap.get(suite)) == null) {
        suiteMap.put(suite, classSet = new HashSet<>());
      }
      classSet.add(testClass);
    }

    suiteList = suiteAnalysis.calculate();
    if (suiteMap.containsKey(defaultSuite)) {
      suiteList.addFirst(defaultSuite);
    }

    for (Suite suite : suiteList) {

    }
  }
}
