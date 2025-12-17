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
 * Represents a unit of work with dependency and priority metadata used by the Sleuth scheduler.
 *
 * @param <A> annotation type that describes the dependency
 * @param <T> payload represented by the dependency (class, method, etc.)
 */
public class Dependency<A extends Annotation, T> {

  private final HashSet<Dependency<A, T>> children = new HashSet<>();
  private final String name;
  private A annotation;
  private T value;
  private Culprit culprit;
  private String[] priorityOn;
  private String[] executeAfter;
  private String[] dependsOn;
  private boolean temporary;
  private boolean permanent;
  private boolean completed;
  private int priority;

  /**
   * Creates an incomplete placeholder dependency that can later be aligned with details.
   *
   * @param name unique name of the dependency node
   */
  public Dependency (String name) {

    this.name = name;

    completed = false;
  }

  /**
   * Creates a fully specified dependency node.
   *
   * @param name         unique name of the dependency node
   * @param annotation   annotation that configured the dependency
   * @param value        payload value associated with the dependency
   * @param priority     ordering priority; higher values run later
   * @param executeAfter optional list of dependencies that must finish before execution
   * @param dependsOn    optional list of hard dependencies that must succeed first
   */
  public Dependency (String name, A annotation, T value, int priority, String[] executeAfter, String[] dependsOn) {

    this.name = name;
    this.annotation = annotation;
    this.value = value;
    this.priority = priority;
    this.executeAfter = executeAfter;
    this.dependsOn = dependsOn;

    completed = true;
  }

  /**
   * Copies dependency details from another node with the same name.
   *
   * @param dependency fully defined dependency to align with
   */
  public void align (Dependency<A, T> dependency) {

    this.annotation = dependency.getAnnotation();
    this.value = dependency.getValue();
    this.priority = dependency.getPriority();
    this.dependsOn = dependency.getDependsOn();

    completed = true;
  }

  /**
   * @return the unique dependency name
   */
  public String getName () {

    return name;
  }

  /**
   * @return the annotation that describes this dependency, or {@code null} if not yet aligned
   */
  public A getAnnotation () {

    return annotation;
  }

  /**
   * @return the payload value associated with the dependency
   */
  public T getValue () {

    return value;
  }

  /**
   * @return execution priority; larger numbers are scheduled later
   */
  public int getPriority () {

    return priority;
  }

  /**
   * @return names of dependencies that must be completed when this node has lower priority
   */
  public String[] getPriorityOn () {

    return priorityOn;
  }

  /**
   * Sets dependencies imposed by a higher-priority tier.
   *
   * @param priorityOn dependency names that must be complete before this one executes
   */
  public void setPriorityOn (String[] priorityOn) {

    this.priorityOn = priorityOn;
  }

  /**
   * @return dependencies that must complete before this node executes when in the same tier
   */
  public String[] getExecuteAfter () {

    return executeAfter;
  }

  /**
   * @return required dependencies that must succeed prior to execution
   */
  public String[] getDependsOn () {

    return dependsOn;
  }

  /**
   * Registers a child dependency that is downstream of this node.
   *
   * @param dependency child node
   */
  public void addChild (Dependency<A, T> dependency) {

    children.add(dependency);
  }

  /**
   * @return current child dependencies
   */
  public HashSet<Dependency<A, T>> getChildren () {

    return children;
  }

  /**
   * @return {@code true} when the node has been fully defined/aligned
   */
  public boolean isCompleted () {

    return completed;
  }

  /**
   * @return recorded culprit propagated from a failed prerequisite, if any
   */
  public Culprit getCulprit () {

    return culprit;
  }

  /**
   * Propagates a culprit from an upstream dependency.
   *
   * @param culprit failure to record
   */
  public void setCulprit (Culprit culprit) {

    this.culprit = culprit;
  }

  /**
   * @return whether this node is currently being visited in dependency resolution
   */
  public boolean isTemporary () {

    return temporary;
  }

  /**
   * Marks the node as temporarily visited to detect cycles.
   */
  public void setTemporary () {

    temporary = true;
  }

  /**
   * Clears the temporary visit marker.
   */
  public void unsetTemporary () {

    temporary = false;
  }

  /**
   * @return whether this node has been fully processed in dependency resolution
   */
  public boolean isPermanent () {

    return permanent;
  }

  /**
   * Marks the node as permanently processed.
   */
  public void setPermanent () {

    permanent = true;
  }

  /**
   * @return hash code derived from the dependency name
   */
  @Override
  public int hashCode () {

    return name.hashCode();
  }

  /**
   * Dependencies are considered equal when their names match.
   *
   * @param obj object to compare
   * @return {@code true} if names are equal
   */
  @Override
  public boolean equals (Object obj) {

    return (obj != null) && (obj instanceof Dependency) && ((name == null) ? (((Dependency<?, ?>)obj).getName() == null) : name.equals(((Dependency<?, ?>)obj).getName()));
  }
}
