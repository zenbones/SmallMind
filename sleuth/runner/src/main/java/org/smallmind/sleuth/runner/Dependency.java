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
import java.util.HashSet;

public class Dependency<A extends Annotation, T> {

  private HashSet<Dependency<A, T>> children = new HashSet<>();
  private A annotation;
  private T value;
  private String[] priorityOn;
  private String[] dependsOn;
  private String name;
  private boolean temporary;
  private boolean permanent;
  private boolean completed;
  private int priority;

  public Dependency (String name) {

    this.name = name;

    completed = false;
  }

  public Dependency (String name, A annotation, T value, int priority, String[] dependsOn) {

    this.name = name;
    this.annotation = annotation;
    this.value = value;
    this.priority = priority;
    this.dependsOn = dependsOn;

    completed = true;
  }

  public void align (Dependency<A, T> dependency) {

    this.annotation = annotation;
    this.value = dependency.getValue();
    this.priority = dependency.getPriority();
    this.dependsOn = dependency.getDependsOn();

    completed = true;
  }

  public String getName () {

    return name;
  }

  public A getAnnotation () {

    return annotation;
  }

  public T getValue () {

    return value;
  }

  public int getPriority () {

    return priority;
  }

  public String[] getPriorityOn () {

    return priorityOn;
  }

  public void setPriorityOn (String[] priorityOn) {

    this.priorityOn = priorityOn;
  }

  public String[] getDependsOn () {

    return dependsOn;
  }

  public void addChild (Dependency<A, T> dependency) {

    children.add(dependency);
  }

  public HashSet<Dependency<A, T>> getChildren () {

    return children;
  }

  public boolean isCompleted () {

    return completed;
  }

  public boolean isTemporary () {

    return temporary;
  }

  public void setTemporary () {

    temporary = true;
  }

  public void unsetTemporary () {

    temporary = false;
  }

  public boolean isPermanent () {

    return permanent;
  }

  public void setPermanent () {

    permanent = true;
  }

  @Override
  public int hashCode () {

    return name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj != null) && (obj instanceof Dependency) && ((name == null) ? (((Dependency<?, ?>)obj).getName() == null) : name.equals(((Dependency<?, ?>)obj).getName()));
  }
}
