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
package org.smallmind.nutsnbolts.layout;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Abstract base for containers that group {@link ParaboxElement}s and participate in
 * {@link ParaboxLayout} measurement and layout, with concrete subclasses providing
 * serial or parallel arrangement behavior.
 *
 * @param <B> the concrete box subtype returned by fluent add methods
 */
public abstract class Box<B extends Box<B>> {

  private final ParaboxLayout layout;
  private final LinkedList<ParaboxElement<?>> elements = new LinkedList<ParaboxElement<?>>();
  private final Class<B> managedClass;

  /**
   * Constructs a box bound to the specified layout and self-typed via the managed class token.
   *
   * @param managedClass the {@link Class} of the concrete subtype, used for fluent return casting
   * @param layout       the owning {@link ParaboxLayout} that coordinates sizing and placement
   */
  protected Box (Class<B> managedClass, ParaboxLayout layout) {

    this.managedClass = managedClass;
    this.layout = layout;
  }

  /**
   * Performs the actual layout pass for contained elements along the specified axis.
   *
   * @param bias                 the axis along which to position elements
   * @param containerPosition    the starting offset along the axis
   * @param containerMeasurement the total space available along the axis
   * @param tailor               the {@link LayoutTailor} that caches measurements and applies positions
   */
  protected abstract void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor);

  /**
   * Returns the minimum measurement this box requires along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the {@link LayoutTailor} used for recursive measurement caching; may be {@code null}
   * @return the minimum size along the axis
   */
  public abstract double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the preferred measurement this box requests along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the {@link LayoutTailor} used for recursive measurement caching; may be {@code null}
   * @return the preferred size along the axis
   */
  public abstract double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the maximum measurement this box can occupy along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the {@link LayoutTailor} used for recursive measurement caching; may be {@code null}
   * @return the maximum size along the axis
   */
  public abstract double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the {@link ParaboxLayout} that owns this box.
   *
   * @return the owning layout
   */
  protected ParaboxLayout getLayout () {

    return layout;
  }

  /**
   * Returns the mutable list of {@link ParaboxElement}s held by this box, intended for use by subclasses.
   *
   * @return the live list of elements in insertion order
   */
  protected LinkedList<ParaboxElement<?>> getElements () {

    return elements;
  }

  /**
   * Adds a native platform component to this box with a rigid (immutable) constraint.
   *
   * @param component the platform component to add
   * @param <C>       the type of the component
   * @return this box for method chaining
   */
  public synchronized <C> B add (C component) {

    add(component, Constraint.immutable());

    return managedClass.cast(this);
  }

  /**
   * Adds a native platform component to this box with the specified constraint.
   *
   * @param component  the platform component to add
   * @param constraint the grow/shrink constraint governing the component's sizing
   * @return this box for method chaining
   */
  public synchronized B add (Object component, Constraint constraint) {

    layout.getContainer().nativelyAddComponent(component);
    elements.add(layout.getContainer().constructElement(component, constraint));

    return managedClass.cast(this);
  }

  /**
   * Adds a nested box to this box with a stretch (grow-and-shrink) constraint.
   *
   * @param box the child box to nest inside this box
   * @return this box for method chaining
   */
  public synchronized B add (Box<?> box) {

    add(box, Constraint.stretch());

    return managedClass.cast(this);
  }

  /**
   * Adds a nested box to this box with the specified constraint.
   *
   * @param box        the child box to nest inside this box
   * @param constraint the grow/shrink constraint governing the nested box's sizing
   * @return this box for method chaining
   */
  public synchronized B add (Box<?> box, Constraint constraint) {

    elements.add(new BoxParaboxElement(box, constraint));

    return managedClass.cast(this);
  }

  /**
   * Removes every element from this box, recursively cleaning up nested boxes and
   * notifying the native container to remove all component references.
   */
  public synchronized void removeAll () {

    Iterator<ParaboxElement<?>> elementIter = elements.iterator();

    while (elementIter.hasNext()) {

      ParaboxElement<?> element = elementIter.next();

      if (element.isNativeComponent()) {
        layout.getContainer().nativelyRemoveComponent(element.getPart());
      } else {
        ((Box)element.getPart()).removeAll();
      }

      elementIter.remove();
    }
  }

  /**
   * Removes the first occurrence of the specified component from this box or any nested descendant box,
   * also notifying the native container.
   *
   * @param component the platform component or nested box to remove
   * @return {@code true} if the component was found and removed; {@code false} otherwise
   */
  public boolean remove (Object component) {

    Iterator<ParaboxElement<?>> elementIter = elements.iterator();

    while (elementIter.hasNext()) {

      ParaboxElement<?> element = elementIter.next();

      if (element.isNativeComponent()) {
        if (element.getPart().equals(component)) {
          layout.getContainer().nativelyRemoveComponent(component);

          elementIter.remove();

          return true;
        }
      } else {
        if (element.getPart().equals(component)) {
          ((Box)element.getPart()).removeAll();

          elementIter.remove();

          return true;
        } else if (((Box)element.getPart()).remove(component)) {

          return true;
        }
      }
    }

    return false;
  }
}
