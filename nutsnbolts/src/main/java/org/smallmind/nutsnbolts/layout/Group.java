/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public abstract class Group<G extends Group<G>> {

  private ParaboxLayout layout;
  private LinkedList<ParaboxElement<?>> elements = new LinkedList<ParaboxElement<?>>();
  private Class<G> managedClass;

  protected Group (Class<G> managedClass, ParaboxLayout layout) {

    this.managedClass = managedClass;
    this.layout = layout;
  }

  protected abstract void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor);

  public abstract double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor);

  public abstract double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor);

  public abstract double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor);

  protected ParaboxLayout getLayout () {

    return layout;
  }

  protected LinkedList<ParaboxElement<?>> getElements () {

    return elements;
  }

  public synchronized <C> G add (C component) {

    add(component, Constraint.immutable());

    return managedClass.cast(this);
  }

  public synchronized G add (Object component, Constraint constraint) {

    getLayout().getContainer().nativelyAddComponent(component);
    elements.add(layout.getContainer().constructElement(component, constraint));

    return managedClass.cast(this);
  }

  public synchronized G add (Group<?> group) {

    add(group, Constraint.stretch());

    return managedClass.cast(this);
  }

  public synchronized G add (Group<?> group, Constraint constraint) {

    elements.add(new GroupParaboxElement(group, constraint));

    return managedClass.cast(this);
  }

  public synchronized void remove (Object component) {

    Iterator<ParaboxElement<?>> elementIter = elements.iterator();

    while (elementIter.hasNext()) {
      if (elementIter.next().getPart().equals(component)) {
        elementIter.remove();
        break;
      }
    }
  }
}
