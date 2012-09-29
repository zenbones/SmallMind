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

public abstract class Group<C, G extends Group> {

  private ParaboxLayout<C> layout;
  private LinkedList<ParaboxElement<?>> elements = new LinkedList<ParaboxElement<?>>();

  protected Group (ParaboxLayout<C> layout) {

    this.layout = layout;
  }

  protected abstract void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor);

  public abstract double calculateMinimumMeasurement (Bias bias);

  public abstract double calculatePreferredMeasurement (Bias bias);

  public abstract double calculateMaximumMeasurement (Bias bias);

  protected ParaboxLayout<C> getLayout () {

    return layout;
  }

  protected LinkedList<ParaboxElement<?>> getElements () {

    return elements;
  }

  public synchronized Group<C, G> add (C component) {

    add(component, ParaboxConstraint.immutable());

    return this;
  }

  public synchronized Group<C, G> add (C component, Spec spec) {

    add(component, spec.staticConstraint());

    return this;
  }

  public synchronized Group<C, G> add (C component, ParaboxConstraint constraint) {

    elements.add(layout.getContainer().constructElement(component, constraint));

    return this;
  }

  public synchronized Group<C, G> add (Group<C, ?> group) {

    add(group, ParaboxConstraint.immutable());

    return this;
  }

  public synchronized Group<C, G> add (Group<C, ?> group, Spec spec) {

    add(group, spec.staticConstraint());

    return this;
  }

  public synchronized Group<C, G> add (Group<C, ?> group, ParaboxConstraint constraint) {

    elements.add(new GroupParaboxElement<Group>(group, constraint));

    return this;
  }

  public synchronized void remove (C component) {

    Iterator<ParaboxElement<?>> elementIter = elements.iterator();

    while (elementIter.hasNext()) {
      if (elementIter.next().getPart().equals(component)) {
        elementIter.remove();
        break;
      }
    }
  }
}
