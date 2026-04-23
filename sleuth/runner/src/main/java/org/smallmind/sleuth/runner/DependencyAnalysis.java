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
 * Builds and validates the dependency graph for a set of annotated work units, then produces a
 * topologically sorted {@link DependencyQueue} ready for concurrent execution.
 * <p>
 * Nodes are added via {@link #add}. Priority tiers are computed automatically: before sorting,
 * every node in a lower-priority tier is wired as a parent of every node in the next tier,
 * ensuring higher-priority work always completes first. Within a tier, {@link Dependency#getDependsOn()}
 * and {@link Dependency#getExecuteAfter()} express finer-grained ordering. Topological ordering is
 * performed by an iterative depth-first search that detects cycles and missing nodes.
 *
 * @param <A> annotation type used to describe each dependency node
 * @param <T> payload type carried by each dependency node
 * @see Dependency
 * @see DependencyQueue
 */
public class DependencyAnalysis<A extends Annotation, T> {

  private final HashMap<String, Dependency<A, T>> dependencyMap = new HashMap<>();
  private final TreeMap<Integer, HashSet<Dependency<A, T>>> priorityMap = new TreeMap<>();
  private final Class<A> annotationClass;

  /**
   * Constructs an analysis for the given annotation type.
   *
   * @param annotationClass annotation type used in error messages when dependency problems are detected;
   *                        must not be {@code null}
   */
  public DependencyAnalysis (Class<A> annotationClass) {

    this.annotationClass = annotationClass;
  }

  /**
   * Adds a dependency node to the graph, merging it with any existing placeholder of the same name.
   * <p>
   * If the node names hard prerequisites in {@link Dependency#getDependsOn()}, those prerequisite nodes
   * are created as placeholders in the graph if not already present, and parent-child edges are
   * established so the sort can traverse them. Duplicate names cause the placeholder to be aligned
   * with the incoming node's details via {@link Dependency#align(Dependency)}.
   *
   * @param dependency node to register; must not be {@code null} and must have a unique, non-null name
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
   * Resolves priority tiers, performs topological sorting, and returns an executable queue.
   * <p>
   * When multiple priority values are present, all nodes in each tier are wired as parents of every
   * node in the subsequent tier, enforcing the tier ordering on top of any explicit
   * {@code dependsOn}/{@code executeAfter} relationships. Sorting uses an iterative DFS; cycles
   * are detected via the temporary marker and missing nodes are identified when an incomplete
   * placeholder has no details after the full graph is built.
   *
   * @return a {@link DependencyQueue} containing all nodes in execution order; never {@code null}
   * @throws TestDependencyException if a cycle is detected in the dependency graph or a named
   *                                 prerequisite does not correspond to any registered node
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
   * Performs a single depth-first traversal step for topological sorting.
   * <p>
   * Children are visited recursively before the node itself is appended to the front of the sorted
   * list (reverse post-order). The temporary marker detects back-edges (cycles); the permanent
   * marker prevents nodes from being re-visited.
   *
   * @param dependency     node currently being visited; must not be {@code null}
   * @param dependencyList accumulates nodes in execution order; must not be {@code null}
   * @param completedSet   collects names of nodes fully processed in this pass for removal from
   *                       the graph; must not be {@code null}
   * @throws TestDependencyException if a cycle is detected ({@code dependency} is already on the
   *                                 stack) or {@code dependency} is an un-aligned placeholder
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
