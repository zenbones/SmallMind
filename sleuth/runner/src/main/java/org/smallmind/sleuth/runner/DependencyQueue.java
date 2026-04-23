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
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Thread-safe execution queue that dispatches dependency nodes only after all their prerequisites
 * have been marked complete.
 * <p>
 * The queue is seeded with a topologically sorted list produced by {@link DependencyAnalysis}. At
 * each call to {@link #poll()}, the list is scanned for any node whose {@code dependsOn},
 * {@code executeAfter}, and {@code priorityOn} prerequisites are all in the completed set. When
 * one is found it is removed from the list and returned. If no eligible node exists the calling
 * thread blocks on {@link Object#wait()} until {@link #complete(Dependency)} wakes it. When the
 * list is empty {@code null} is returned, signalling end of queue.
 * <p>
 * This class is thread-safe; both {@link #poll()} and {@link #complete(Dependency)} are
 * {@code synchronized}.
 *
 * @param <A> annotation type describing each dependency node
 * @param <T> payload type carried by each dependency node
 * @see DependencyAnalysis
 * @see Dependency
 */
public class DependencyQueue<A extends Annotation, T> {

  private final LinkedList<Dependency<A, T>> dependencyList;
  private final HashMap<String, Dependency<A, T>> dependencyMap = new HashMap<>();
  private final HashSet<String> completedSet = new HashSet<>();
  private final int size;

  /**
   * Creates a queue from a topologically ordered dependency list.
   * <p>
   * All nodes are indexed by name so that culprit propagation can look them up in O(1) time.
   *
   * @param dependencyList topologically sorted list of dependencies; must not be {@code null}
   */
  public DependencyQueue (LinkedList<Dependency<A, T>> dependencyList) {

    this.dependencyList = dependencyList;

    size = dependencyList.size();
    for (Dependency<A, T> dependency : dependencyList) {
      dependencyMap.put(dependency.getName(), dependency);
    }
  }

  /**
   * @return the total number of nodes in the queue, including those not yet dispatched
   */
  public int size () {

    return size;
  }

  /**
   * Retrieves and removes the next node whose prerequisites are all complete, blocking if none
   * is currently eligible.
   * <p>
   * The internal list is scanned on each wakeup until an eligible node is found or the list
   * becomes empty. Culprit propagation from failed hard prerequisites occurs inside the
   * eligibility check.
   *
   * @return the next eligible dependency, or {@code null} when all nodes have been dispatched
   * @throws RuntimeException wrapping {@link InterruptedException} if the waiting thread is interrupted
   */
  public synchronized Dependency<A, T> poll () {

    while (!dependencyList.isEmpty()) {

      Iterator<Dependency<A, T>> dependencyIter = dependencyList.iterator();

      while (dependencyIter.hasNext()) {

        Dependency<A, T> dependency;

        if (isComplete(dependency = dependencyIter.next())) {
          dependencyIter.remove();

          return dependency;
        }
      }

      try {
        wait();
      } catch (InterruptedException interruptedException) {
        throw new RuntimeException(interruptedException);
      }
    }

    return null;
  }

  /**
   * Records a node as complete and wakes all threads waiting in {@link #poll()}.
   *
   * @param dependency node that has finished executing; must not be {@code null}
   */
  public synchronized void complete (Dependency<A, T> dependency) {

    completedSet.add(dependency.getName());
    notifyAll();
  }

  /**
   * Determines whether all prerequisites for a node have been satisfied.
   * <p>
   * All three prerequisite arrays — {@code dependsOn}, {@code executeAfter}, and
   * {@code priorityOn} — must be fully present in the completed set. For {@code dependsOn},
   * the culprit from any failed prerequisite is propagated to the candidate node.
   *
   * @param dependency node to evaluate; must not be {@code null}
   * @return {@code true} if the node may now be dispatched
   */
  private boolean isComplete (Dependency<A, T> dependency) {

    if (dependency.getDependsOn() != null) {
      for (String requirement : dependency.getDependsOn()) {
        if (!completedSet.contains(requirement)) {

          return false;
        } else if (dependencyMap.get(requirement).getCulprit() != null) {
          dependency.setCulprit(dependencyMap.get(requirement).getCulprit());
        }
      }
    }
    if (dependency.getExecuteAfter() != null) {
      for (String requirement : dependency.getExecuteAfter()) {
        if (!completedSet.contains(requirement)) {

          return false;
        }
      }
    }
    if (dependency.getPriorityOn() != null) {
      for (String requirement : dependency.getPriorityOn()) {
        if (!completedSet.contains(requirement)) {

          return false;
        }
      }
    }

    return true;
  }
}
