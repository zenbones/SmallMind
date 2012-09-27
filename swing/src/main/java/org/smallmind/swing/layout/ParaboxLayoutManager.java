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
import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Pair;
import org.smallmind.nutsnbolts.layout.ParaboxConstraint;
import org.smallmind.nutsnbolts.layout.ParaboxContainer;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;
import org.smallmind.nutsnbolts.layout.Platform;

public class ParaboxLayoutManager implements ParaboxContainer, LayoutManager2 {

  private static final Platform PLATFORM = new SwingParaboxPlatform();

  private final ParaboxLayout<SwingParaboxElement> paraboxLayout;

  private final LinkedList<SwingParaboxElement> elements = new LinkedList<SwingParaboxElement>();

  public ParaboxLayoutManager () {

    this(Bias.HORIZONTAL);
  }

  public ParaboxLayoutManager (Bias bias) {

    this(bias, Gap.RELATED);
  }

  public ParaboxLayoutManager (Bias bias, Gap gap) {

    this(bias, gap.getGap(PLATFORM));
  }

  public ParaboxLayoutManager (Bias bias, double gap) {

    this(bias, gap, Alignment.LEADING, Alignment.CENTER);
  }

  public ParaboxLayoutManager (Bias bias, Gap gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    this(bias, gap.getGap(PLATFORM), biasedAlignment, unbiasedAlignment);
  }

  public ParaboxLayoutManager (Bias bias, double gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    paraboxLayout = new ParaboxLayout<SwingParaboxElement>(this, bias, gap, biasedAlignment, unbiasedAlignment);
  }

  @Override
  public Platform getPlatform () {

    return PLATFORM;
  }

  @Override
  public void addLayoutComponent (Component comp, Object constraints) {

    elements.add(new SwingParaboxElement(comp, (ParaboxConstraint)constraints));
  }

  @Override
  public void addLayoutComponent (String name, Component comp) {

    elements.add(new SwingParaboxElement(comp));
  }

  @Override
  public void removeLayoutComponent (Component comp) {

    Iterator<SwingParaboxElement> elementIter = elements.iterator();

    while (elementIter.hasNext()) {
      if (elementIter.next().getComponent().equals(comp)) {
        elementIter.remove();
        break;
      }
    }
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
  public Dimension preferredLayoutSize (Container parent) {

    Pair size = paraboxLayout.calculatePreferredContainerSize(elements);

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension minimumLayoutSize (Container parent) {

    Pair size = paraboxLayout.calculateMinimumContainerSize(elements);

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public Dimension maximumLayoutSize (Container target) {

    Pair size = paraboxLayout.calculateMaximumContainerSize(elements);

    return new Dimension((int)size.getFirst(), (int)size.getSecond());
  }

  @Override
  public void invalidateLayout (Container target) {
  }

  @Override
  public void layoutContainer (Container parent) {

    paraboxLayout.doLayout(parent.getWidth(), parent.getHeight(), elements);
  }
}
