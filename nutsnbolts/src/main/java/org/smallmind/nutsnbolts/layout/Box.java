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
 * Base container for grouping {@link ParaboxElement}s in parallel or serial arrangements.
 * Subclasses implement measurement and layout behaviors for their specific arrangement.
 *
 * @param <B> the concrete box subtype used for fluent APIs
 */
public abstract class Box<B extends Box<B>> {

  private final ParaboxLayout layout;
  private final LinkedList<ParaboxElement<?>> elements = new LinkedList<ParaboxElement<?>>();
  private final Class<B> managedClass;

  /**
   * Creates a box backed by the given layout and using the supplied managed class for fluency.
   *
   * @param managedClass the concrete class returned by fluent methods
   * @param layout       the layout controller coordinating this box
   */
  protected Box (Class<B> managedClass, ParaboxLayout layout) {

    this.managedClass = managedClass;
    this.layout = layout;
  }

  /**
   * Performs layout of contained elements along the given axis.
   *
   * @param bias                 the axis to lay out against
   * @param containerPosition    the starting position along the axis
   * @param containerMeasurement the available measurement along the axis
   * @param tailor               the layout tailor used to size elements
   */
  protected abstract void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor);

  /**
   * Calculates the minimum size required along the provided axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor used for sizing
   * @return the minimum measurement
   */
  public abstract double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Calculates the preferred size along the provided axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor used for sizing
   * @return the preferred measurement
   */
  public abstract double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Calculates the maximum size along the provided axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor used for sizing
   * @return the maximum measurement
   */
  public abstract double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the owning layout.
   *
   * @return the layout controlling this box
   */
  protected ParaboxLayout getLayout () {

    return layout;
  }

  /**
   * Returns the internal list of elements for subclasses to manipulate.
   *
   * @return the contained elements
   */
  protected LinkedList<ParaboxElement<?>> getElements () {

    return elements;
  }

  /**
   * Adds a native component with an immutable constraint.
   *
   * @param component the component to add
   * @param <C>       component type
   * @return this box for chaining
   */
  public synchronized <C> B add (C component) {

    add(component, Constraint.immutable());

    return managedClass.cast(this);
  }

  /**
   * Adds a native component with the supplied constraint.
   *
   * @param component  the component to add
   * @param constraint sizing and alignment constraint
   * @return this box for chaining
   */
  public synchronized B add (Object component, Constraint constraint) {

    layout.getContainer().nativelyAddComponent(component);
    elements.add(layout.getContainer().constructElement(component, constraint));

    return managedClass.cast(this);
  }

  /**
   * Adds a nested box with a stretch constraint.
   *
   * @param box the child box to add
   * @return this box for chaining
   */
  public synchronized B add (Box<?> box) {

    add(box, Constraint.stretch());

    return managedClass.cast(this);
  }

  /**
   * Adds a nested box with the provided constraint.
   *
   * @param box        the child box to add
   * @param constraint sizing and alignment constraint
   * @return this box for chaining
   */
  public synchronized B add (Box<?> box, Constraint constraint) {

    elements.add(new BoxParaboxElement(box, constraint));

    return managedClass.cast(this);
  }

  /**
   * Removes all elements (and nested components/boxes) from this box.
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
   * Removes the first occurrence of the specified component from this box or its descendants.
   *
   * @param component the component to remove
   * @return {@code true} if removed from this box or a nested box
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
