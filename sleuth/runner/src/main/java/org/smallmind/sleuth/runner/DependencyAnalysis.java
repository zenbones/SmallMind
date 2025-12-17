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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds and validates a dependency graph for annotated elements.
 * <p>
 * Dependencies are ordered by priority and explicit parent/child relationships and are converted into
 * a {@link DependencyQueue} suitable for concurrent execution while respecting dependencies.
 *
 * @param <A> annotation type describing a dependency
 * @param <T> payload type represented by the dependency node
 */
public class DependencyAnalysis<A extends Annotation, T> {

  private final HashMap<String, Dependency<A, T>> dependencyMap = new HashMap<>();
  private final TreeMap<Integer, HashSet<Dependency<A, T>>> priorityMap = new TreeMap<>();
  private final Class<A> annotationClass;

  /**
   * @param annotationClass annotation type used when reporting dependency errors
   */
  public DependencyAnalysis (Class<A> annotationClass) {

    this.annotationClass = annotationClass;
  }

  /**
   * Adds a dependency node to the graph, linking children to parents and recording priority.
   *
   * @param dependency dependency node to add or merge
   */
  public void add (Dependency<A, T> dependency) {

    Dependency<A, T> mappedDependency;
    HashSet<Dependency<A, T>> dependencySet;

    if ((mappedDependency = dependencyMap.putIfAbsent(dependency.getName(), dependency)) == null) {
      mappedDependency = dependency;
    } else {
      mappedDependency.align(dependency);
    }

    if ((dependencySet = priorityMap.get(dependency.getPriority())) == null) {
      priorityMap.put(dependency.getPriority(), dependencySet = new HashSet<>());
    }
    dependencySet.add(dependency);

    if ((mappedDependency.getDependsOn() != null) && (mappedDependency.getDependsOn().length > 0)) {
      for (String parentName : mappedDependency.getDependsOn()) {

        Dependency<A, T> parentDependency;

        if ((parentDependency = dependencyMap.get(parentName)) == null) {
          dependencyMap.put(parentName, parentDependency = new Dependency<>(parentName));
        }
        parentDependency.addChild(mappedDependency);
      }
    }
  }

  /**
   * Performs dependency analysis and returns an executable queue.
   *
   * @return a queue ordered for execution that respects dependency constraints
   * @throws TestDependencyException when missing or cyclic dependencies are detected
   */
  public DependencyQueue<A, T> calculate () {

    LinkedList<Dependency<A, T>> calculatedDependencyList = new LinkedList<>();

    if (priorityMap.size() > 1) {

      HashSet<Dependency<A, T>> priorDependencySet = null;

      for (Map.Entry<Integer, HashSet<Dependency<A, T>>> priorityEntry : priorityMap.entrySet()) {
        if (priorDependencySet != null) {

          String[] priorNames;
          int nameIndex = 0;

          priorNames = new String[priorDependencySet.size()];
          for (Dependency<A, T> priorDependency : priorDependencySet) {
            priorNames[nameIndex++] = priorDependency.getName();
          }

          for (Dependency<A, T> subsequentDependency : priorityEntry.getValue()) {
            subsequentDependency.setPriorityOn(priorNames);
            for (Dependency<A, T> priorDependency : priorDependencySet) {
              priorDependency.addChild(subsequentDependency);
            }
          }
        }

        priorDependencySet = priorityEntry.getValue();
      }
    }

    while (!dependencyMap.isEmpty()) {

      HashSet<String> completedSet = new HashSet<>();

      for (Dependency<A, T> dependency : dependencyMap.values()) {
        visit(dependency, calculatedDependencyList, completedSet);
      }
      for (String name : completedSet) {
        dependencyMap.remove(name);
      }
    }

    return new DependencyQueue<>(calculatedDependencyList);
  }

  /**
   * Depth-first traversal used to detect cycles and build a topologically sorted list.
   *
   * @param dependency     current node
   * @param dependencyList ordered dependency list being built
   * @param completedSet   names of nodes already finalized and removed from the map
   */
  private void visit (Dependency<A, T> dependency, LinkedList<Dependency<A, T>> dependencyList, HashSet<String> completedSet) {

    if (dependency.isTemporary()) {
      throw new TestDependencyException("Cyclic dependency(%s) detected involving node(%s)", annotationClass.getSimpleName(), dependency.getName());
    }
    if (!(dependency.isTemporary() || dependency.isPermanent())) {
      dependency.setTemporary();
      for (Dependency<A, T> childDependency : dependency.getChildren()) {
        visit(childDependency, dependencyList, completedSet);
      }
      if (!dependency.isCompleted()) {
        throw new TestDependencyException("Missing dependency(%s) on node(%s)", annotationClass.getSimpleName(), dependency.getName());
      }
      dependency.setPermanent();
      dependency.unsetTemporary();
      dependencyList.addFirst(dependency);
      completedSet.add(dependency.getName());
    }
  }
}
