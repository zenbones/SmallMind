/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Box;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.LayoutException;
import org.smallmind.nutsnbolts.layout.Pair;
import org.smallmind.nutsnbolts.layout.ParaboxContainer;
import org.smallmind.nutsnbolts.layout.ParaboxElement;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.ParallelBox;
import org.smallmind.nutsnbolts.layout.SerialBox;

public class ParaboxLayoutManager implements ParaboxContainer<Component>, LayoutManager2 {

  private static final ParaboxPlatform PLATFORM = new SwingParaboxPlatform();

  private final Container container;
  private final ParaboxLayout paraboxLayout;

  public ParaboxLayoutManager (Container container) {

    this.container = container;

    paraboxLayout = new ParaboxLayout(this);
  }

  @Override
  public ParaboxPlatform getPlatform () {

    return PLATFORM;
  }

  @Override
  public void addLayoutComponent (Component comp, Object constraints) {

  }

  @Override
  public void addLayoutComponent (String name, Component comp) {

  }

  public void removeAll () {

    paraboxLayout.removeAll();
  }

  @Override
  public void removeLayoutComponent (Component comp) {

    paraboxLayout.remove(comp);
  }

  @Override
  public float getLayoutAlignmentX (Container target) {

    if (target != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    return 0.5F;
  }

  @Override
  public float getLayoutAlignmentY (Container target) {

    if (target != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    return 0.5F;
  }

  @Override
  public void nativelyAddComponent (Component component) {

    boolean matched = false;

    for (Component containedComponent : container.getComponents()) {
      if (containedComponent == component) {
        matched = true;
        break;
      }
    }

    if (!matched) {
      container.add(component);
    }
  }

  @Override
  public void nativelyRemoveComponent (Component component) {

    container.remove(component);
  }

  @Override
  public ParaboxElement<Component> constructElement (Component component, Constraint constraint) {

    return new SwingParaboxElement(component, constraint);
  }

  @Override
  public Dimension preferredLayoutSize (Container parent) {

    if (parent != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    Pair size = paraboxLayout.calculatePreferredSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension minimumLayoutSize (Container parent) {

    if (parent != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    Pair size = paraboxLayout.calculateMinimumSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension maximumLayoutSize (Container parent) {

    if (parent != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    Pair size = paraboxLayout.calculateMaximumSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public void invalidateLayout (Container parent) {

    if (parent != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }
  }

  @Override
  public void layoutContainer (Container parent) {

    if (parent != container) {
      throw new LayoutException("All layout operations must occur upon this manager's parent container");
    }

    paraboxLayout.doLayout(parent.getWidth(), parent.getHeight(), parent.getComponents());
  }

  public Box<?> getHorizontalBox () {

    return paraboxLayout.getHorizontalBox();
  }

  public ParaboxLayoutManager setHorizontalBox (Box<?> horizontalBox) {

    paraboxLayout.setHorizontalBox(horizontalBox);

    return this;
  }

  public Box<?> getVerticalBox () {

    return paraboxLayout.getVerticalBox();
  }

  public ParaboxLayoutManager setVerticalBox (Box<?> verticalBox) {

    paraboxLayout.setVerticalBox(verticalBox);

    return this;
  }

  public ParallelBox parallelBox () {

    return paraboxLayout.parallelBox();
  }

  public ParallelBox parallelBox (Alignment alignment) {

    return paraboxLayout.parallelBox(alignment);
  }

  public SerialBox sequentialBox () {

    return paraboxLayout.serialBox();
  }

  public SerialBox sequentialBox (boolean greedy) {

    return paraboxLayout.serialBox(greedy);
  }

  public SerialBox sequentialBox (Gap gap) {

    return paraboxLayout.serialBox(gap);
  }

  public SerialBox sequentialBox (Gap gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  public SerialBox sequentialBox (double gap) {

    return paraboxLayout.serialBox(gap);
  }

  public SerialBox sequentialBox (double gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  public SerialBox sequentialBox (Justification justification) {

    return paraboxLayout.serialBox(justification);
  }

  public SerialBox sequentialBox (Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(justification, greedy);
  }

  public SerialBox sequentialBox (Gap gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  public SerialBox sequentialBox (Gap gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }

  public SerialBox sequentialBox (double gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  public SerialBox sequentialBox (double gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }
}
