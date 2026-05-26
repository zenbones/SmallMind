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
import java.util.HashSet;

/**
 * Node in the Sleuth dependency graph representing a unit of work with ordering and prerequisite constraints.
 * <p>
 * Dependencies are built by {@link DependencyAnalysis}, sorted topologically, and consumed by
 * {@link DependencyQueue}. Each node carries the annotation that described it, the payload (a class for
 * suite-level nodes or a {@link java.lang.reflect.Method} for test-level nodes), and three kinds of
 * prerequisite relationships:
 * <ul>
 *   <li>{@link #dependsOn} — hard: the node is skipped if any named prerequisite failed.</li>
 *   <li>{@link #executeAfter} — soft: the node waits for named predecessors but is not affected by their outcomes.</li>
 *   <li>{@link #priorityOn} — injected by the scheduler: nodes from a lower-priority tier must all complete first.</li>
 * </ul>
 * A node begins in an <em>incomplete</em> state when created as a forward-reference placeholder and
 * becomes complete either on construction via the full-argument constructor or via {@link #align}.
 * <p>
 * During topological sorting, {@link #setTemporary()}/{@link #unsetTemporary()} mark nodes that are
 * currently on the DFS stack (used for cycle detection), and {@link #setPermanent()} marks nodes that
 * have been fully processed.
 *
 * @param <A> annotation type that configured this node (e.g., {@link org.smallmind.sleuth.runner.annotation.Suite} or
 *            {@link org.smallmind.sleuth.runner.annotation.Test})
 * @param <T> payload type carried by this node (e.g., {@link Class} or {@link java.lang.reflect.Method})
 * @see DependencyAnalysis
 * @see DependencyQueue
 */
public class Dependency<A extends Annotation, T> {

  private final HashSet<Dependency<A, T>> children = new HashSet<>();
  private final String name;
  private A annotation;
  private T value;
  private Culprit culprit;
  private Class<?>[] expectedExceptions;
  private String[] priorityOn;
  private String[] executeAfter;
  private String[] dependsOn;
  private boolean temporary;
  private boolean permanent;
  private boolean completed;
  private int priority;

  /**
   * Creates an incomplete placeholder node for forward-references during graph construction.
   * <p>
   * This constructor is used when a dependency is mentioned by name before its details are known.
   * The node transitions to complete once {@link #align(Dependency)} is called with the full definition.
   *
   * @param name unique name that identifies this node within the graph; must not be {@code null}
   */
  public Dependency (String name) {

    this.name = name;

    completed = false;
  }

  /**
   * Creates a fully specified, complete dependency node.
   *
   * @param name               unique name identifying this node within the graph; must not be {@code null}
   * @param annotation         annotation instance that declared this dependency; may be {@code null}
   * @param value              payload associated with this node (e.g., the class or method to execute)
   * @param priority           scheduling priority; lower values are scheduled before higher values
   * @param executeAfter       names of nodes that must finish (regardless of outcome) before this one starts;
   *                           may be {@code null} or empty
   * @param dependsOn          names of nodes that must succeed before this one starts; failure of any
   *                           named node propagates its culprit here; may be {@code null} or empty
   * @param expectedExceptions exception types this node's payload is expected to throw; the test fails if
   *                           none or a different exception is thrown; may be {@code null} or empty
   */
  public Dependency (String name, A annotation, T value, int priority, String[] executeAfter, String[] dependsOn, Class<?>[] expectedExceptions) {

    this.name = name;
    this.annotation = annotation;
    this.value = value;
    this.priority = priority;
    this.executeAfter = executeAfter;
    this.dependsOn = dependsOn;
    this.expectedExceptions = expectedExceptions;

    completed = true;
  }

  /**
   * Fills in the details of a placeholder node from a fully defined node with the same name.
   * <p>
   * After alignment the node is marked complete. Only the annotation, value, priority,
   * {@code dependsOn}, and {@code expectedExceptions} fields are copied; children accumulated
   * during graph construction are preserved.
   *
   * @param dependency fully defined source node to copy details from; must not be {@code null}
   */
  public void align (Dependency<A, T> dependency) {

    this.annotation = dependency.getAnnotation();
    this.value = dependency.getValue();
    this.priority = dependency.getPriority();
    this.dependsOn = dependency.getDependsOn();
    this.expectedExceptions = dependency.expectedExceptions;

    completed = true;
  }

  /**
   * @return unique name of this node within its dependency graph; never {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * @return annotation instance that described this dependency, or {@code null} when not yet aligned
   */
  public A getAnnotation () {

    return annotation;
  }

  /**
   * @return payload value to execute when this node is dispatched; may be {@code null} for placeholders
   */
  public T getValue () {

    return value;
  }

  /**
   * @return scheduling priority; lower values execute before higher values
   */
  public int getPriority () {

    return priority;
  }

  /**
   * @return names of nodes that must complete due to a higher priority tier before this node may start;
   * {@code null} until set by {@link DependencyAnalysis}
   */
  public String[] getPriorityOn () {

    return priorityOn;
  }

  /**
   * Sets the cross-tier priority ordering constraint injected by {@link DependencyAnalysis}.
   *
   * @param priorityOn names of lower-priority-tier nodes that must all complete first;
   *                   may be {@code null}
   */
  public void setPriorityOn (String[] priorityOn) {

    this.priorityOn = priorityOn;
  }

  /**
   * @return names of nodes that must complete (soft ordering) before this node may start;
   * may be {@code null} or empty
   */
  public String[] getExecuteAfter () {

    return executeAfter;
  }

  /**
   * @return names of nodes that must succeed (hard dependency) before this node may start;
   * may be {@code null} or empty
   */
  public String[] getDependsOn () {

    return dependsOn;
  }

  /**
   * @return exception types this node's payload is expected to throw; the test fails if none or a
   * different exception is thrown; may be {@code null} or empty
   */
  public Class<?>[] getExpectedExceptions () {

    return expectedExceptions;
  }

  /**
   * Registers a downstream node that must wait for this node to complete before it can execute.
   *
   * @param dependency child node to add; must not be {@code null}
   */
  public void addChild (Dependency<A, T> dependency) {

    children.add(dependency);
  }

  /**
   * @return set of nodes that are downstream of this one; never {@code null}
   */
  public HashSet<Dependency<A, T>> getChildren () {

    return children;
  }

  /**
   * @return {@code true} when this node has been fully defined, either via the full constructor or
   * via {@link #align(Dependency)}
   */
  public boolean isCompleted () {

    return completed;
  }

  /**
   * @return culprit propagated from a failed prerequisite, or {@code null} if none
   */
  public Culprit getCulprit () {

    return culprit;
  }

  /**
   * Records a culprit from a failed prerequisite so that downstream nodes can be skipped.
   *
   * @param culprit failure origin to propagate; may be {@code null} to clear
   */
  public void setCulprit (Culprit culprit) {

    this.culprit = culprit;
  }

  /**
   * @return {@code true} if this node is currently on the DFS stack during topological sorting
   * (cycle detection marker)
   */
  public boolean isTemporary () {

    return temporary;
  }

  /**
   * Marks this node as currently being visited during topological sort (cycle detection).
   */
  public void setTemporary () {

    temporary = true;
  }

  /**
   * Clears the temporary-visit marker after the DFS recursion unwinds from this node.
   */
  public void unsetTemporary () {

    temporary = false;
  }

  /**
   * @return {@code true} if this node has been fully processed and added to the sorted output
   */
  public boolean isPermanent () {

    return permanent;
  }

  /**
   * Marks this node as permanently processed, preventing it from being visited again.
   */
  public void setPermanent () {

    permanent = true;
  }

  /**
   * @return hash code derived from the node's name for use in hash-based collections
   */
  @Override
  public int hashCode () {

    return name.hashCode();
  }

  /**
   * Two dependency nodes are considered equal when their names match, regardless of payload.
   *
   * @param obj object to compare; may be {@code null}
   * @return {@code true} if both nodes have the same name
   */
  @Override
  public boolean equals (Object obj) {

    return (obj != null) && (obj instanceof Dependency) && ((name == null) ? (((Dependency<?, ?>)obj).getName() == null) : name.equals(((Dependency<?, ?>)obj).getName()));
  }
}
