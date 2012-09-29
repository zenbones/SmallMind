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

import java.util.Arrays;
import java.util.List;

public class ParaboxLayout<C> {

  private ParaboxContainer<C> container;
  private Group horizontalGroup;
  private Group verticalGroup;

  public ParaboxLayout (ParaboxContainer<C> container) {

    this.container = container;
  }

  public ParaboxContainer<C> getContainer () {

    return container;
  }

  public Group getHorizontalGroup () {

    return horizontalGroup;
  }

  public ParaboxLayout setHorizontalGroup (Group horizontalGroup) {

    this.horizontalGroup = horizontalGroup;

    return this;
  }

  public Group getVerticalGroup () {

    return verticalGroup;
  }

  public ParaboxLayout setVerticalGroup (Group verticalGroup) {

    this.verticalGroup = verticalGroup;

    return this;
  }

  public Pair calculateMinimumSize () {

    return new Pair(horizontalGroup.calculateMinimumMeasurement(Bias.HORIZONTAL), verticalGroup.calculateMinimumMeasurement(Bias.VERTICAL));
  }

  public Pair calculatePreferredSize () {

    return new Pair(horizontalGroup.calculatePreferredMeasurement(Bias.HORIZONTAL), verticalGroup.calculatePreferredMeasurement(Bias.VERTICAL));
  }

  public Pair calculateMaximumSize () {

    return new Pair(horizontalGroup.calculateMaximumMeasurement(Bias.HORIZONTAL), verticalGroup.calculateMaximumMeasurement(Bias.VERTICAL));
  }

  public void doLayout (double width, double height, C... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  public void doLayout (double width, double height, List<C> componentList) {

    LayoutTailor tailor;

    if (horizontalGroup == null) {
      throw new LayoutException("No horizontal group has been set on this layout");
    }
    if (verticalGroup == null) {
      throw new LayoutException("No vertical group has been set on this layout");
    }

    horizontalGroup.doLayout(Bias.HORIZONTAL, 0, width, tailor = new LayoutTailor(componentList));
    verticalGroup.doLayout(Bias.VERTICAL, 0, height, tailor);

    tailor.cleanup();
  }

  public ParallelGroup<C> parallel () {

    return new ParallelGroup<C>(this);
  }

  public ParallelGroup<C> parallel (Alignment alignment) {

    return new ParallelGroup<C>(this, alignment);
  }

  public SequentialGroup<C> sequential () {

    return new SequentialGroup<C>(this);
  }

  public SequentialGroup<C> sequential (Gap gap) {

    return new SequentialGroup<C>(this, gap);
  }

  public SequentialGroup<C> sequential (double gap) {

    return new SequentialGroup<C>(this, gap);
  }

  public SequentialGroup<C> sequential (Justification justification) {

    return new SequentialGroup<C>(this, justification);
  }

  public SequentialGroup<C> sequential (Gap gap, Justification justification) {

    return new SequentialGroup<C>(this, gap, justification);
  }

  public SequentialGroup<C> sequential (double gap, Justification justification) {

    return new SequentialGroup<C>(this, gap, justification);
  }
}