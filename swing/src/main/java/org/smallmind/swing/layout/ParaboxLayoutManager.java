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
package org.smallmind.swing.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Group;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.Pair;
import org.smallmind.nutsnbolts.layout.ParaboxConstraint;
import org.smallmind.nutsnbolts.layout.ParaboxContainer;
import org.smallmind.nutsnbolts.layout.ParaboxElement;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;
import org.smallmind.nutsnbolts.layout.ParallelGroup;
import org.smallmind.nutsnbolts.layout.Platform;
import org.smallmind.nutsnbolts.layout.SequentialGroup;

public class ParaboxLayoutManager implements ParaboxContainer<Component>, LayoutManager2 {

  private static final Platform PLATFORM = new SwingParaboxPlatform();

  private final ParaboxLayout<Component> paraboxLayout;

  public ParaboxLayoutManager () {

    paraboxLayout = new ParaboxLayout<Component>(this);
  }

  @Override
  public Platform getPlatform () {

    return PLATFORM;
  }

  @Override
  public void addLayoutComponent (Component comp, Object constraints) {

  }

  @Override
  public void addLayoutComponent (String name, Component comp) {

  }

  @Override
  public void removeLayoutComponent (Component comp) {

  }

  @Override
  public float getLayoutAlignmentX (Container target) {

    return 0.5F;
  }

  @Override
  public float getLayoutAlignmentY (Container target) {

    return 0.5F;
  }

  @Override
  public ParaboxElement<Component> constructElement (Component component, ParaboxConstraint constraint) {

    return new SwingParaboxElement(component, constraint);
  }

  @Override
  public Dimension preferredLayoutSize (Container parent) {

    Pair size = paraboxLayout.calculatePreferredSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension minimumLayoutSize (Container parent) {

    Pair size = paraboxLayout.calculateMinimumSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension maximumLayoutSize (Container target) {

    Pair size = paraboxLayout.calculateMaximumSize();

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public void invalidateLayout (Container target) {

  }

  @Override
  public void layoutContainer (Container parent) {

    paraboxLayout.doLayout(parent.getWidth(), parent.getHeight(), parent.getComponents());
  }

  public ParaboxLayoutManager setHorizontalGroup (Group horizontalGroup) {

    paraboxLayout.setHorizontalGroup(horizontalGroup);

    return this;
  }

  public ParaboxLayoutManager setVerticalGroup (Group verticalGroup) {

    paraboxLayout.setVerticalGroup(verticalGroup);

    return this;
  }

  public ParallelGroup<Component> parallel () {

    return paraboxLayout.parallel();
  }

  public ParallelGroup<Component> parallel (Alignment alignment) {

    return paraboxLayout.parallel(alignment);
  }

  public SequentialGroup<Component> sequential () {

    return paraboxLayout.sequential();
  }

  public SequentialGroup<Component> sequential (Gap gap) {

    return paraboxLayout.sequential(gap);
  }

  public SequentialGroup<Component> sequential (double gap) {

    return paraboxLayout.sequential(gap);
  }

  public SequentialGroup<Component> sequential (Justification justification) {

    return paraboxLayout.sequential(justification);
  }

  public SequentialGroup<Component> sequential (Gap gap, Justification justification) {

    return paraboxLayout.sequential(gap, justification);
  }

  public SequentialGroup<Component> sequential (double gap, Justification justification) {

    return paraboxLayout.sequential(gap, justification);
  }
}
