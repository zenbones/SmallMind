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
package org.smallmind.javafx.layout;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Box;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.ParaboxContainer;
import org.smallmind.nutsnbolts.layout.ParaboxElement;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.ParallelBox;
import org.smallmind.nutsnbolts.layout.SequentialBox;

public class ParaboxPane extends Region implements ParaboxContainer<Node> {

  private static final ParaboxPlatform PLATFORM = new JavaFxParaboxPlatform();

  private final ParaboxLayout paraboxLayout;

  public ParaboxPane () {

    paraboxLayout = new ParaboxLayout(this);
  }

  @Override
  public ParaboxPlatform getPlatform () {

    return PLATFORM;
  }

  @Override
  protected double computeMinWidth (double v) {

    return paraboxLayout.calculateMinimumWidth();
  }

  @Override
  protected double computeMinHeight (double v) {

    return paraboxLayout.calculateMinimumWidth();
  }

  @Override
  protected double computePrefWidth (double v) {

    return paraboxLayout.calculatePreferredWidth();
  }

  @Override
  protected double computePrefHeight (double v) {

    return paraboxLayout.calculatePreferredWidth();
  }

  @Override
  protected double computeMaxWidth (double v) {

    return paraboxLayout.calculateMaximumWidth();
  }

  @Override
  protected double computeMaxHeight (double v) {

    return paraboxLayout.calculateMaximumWidth();
  }

  @Override
  protected void layoutChildren () {

    paraboxLayout.doLayout(getWidth(), getHeight(), getChildrenUnmodifiable());
  }

  @Override
  public ParaboxElement<Node> constructElement (Node node, Constraint constraint) {

    return new JavaFxParaboxElement(node, constraint);
  }

  @Override
  public void nativelyAddComponent (Node node) {

    List<Node> children;

    if (!(children = getChildren()).contains(node)) {
      children.add(node);
    }
  }

  public Box<?> getHorizontalBox () {

    return paraboxLayout.getHorizontalBox();
  }

  public ParaboxPane setHorizontalBox (Box<?> horizontalBox) {

    paraboxLayout.setHorizontalBox(horizontalBox);

    return this;
  }

  public Box<?> getVerticalBox () {

    return paraboxLayout.getVerticalBox();
  }

  public ParaboxPane setVerticalBox (Box<?> verticalBox) {

    paraboxLayout.setVerticalBox(verticalBox);

    return this;
  }

  public ParallelBox parallelBox () {

    return paraboxLayout.parallelBox();
  }

  public ParallelBox parallelBox (Alignment alignment) {

    return paraboxLayout.parallelBox(alignment);
  }

  public SequentialBox sequentialBox () {

    return paraboxLayout.sequentialBox();
  }

  public SequentialBox sequentialBox (boolean greedy) {

    return paraboxLayout.sequentialBox(greedy);
  }

  public SequentialBox sequentialBox (Gap gap) {

    return paraboxLayout.sequentialBox(gap);
  }

  public SequentialBox sequentialBox (Gap gap, boolean greedy) {

    return paraboxLayout.sequentialBox(gap, greedy);
  }

  public SequentialBox sequentialBox (double gap) {

    return paraboxLayout.sequentialBox(gap);
  }

  public SequentialBox sequentialBox (double gap, boolean greedy) {

    return paraboxLayout.sequentialBox(gap, greedy);
  }

  public SequentialBox sequentialBox (Justification justification) {

    return paraboxLayout.sequentialBox(justification);
  }

  public SequentialBox sequentialBox (Justification justification, boolean greedy) {

    return paraboxLayout.sequentialBox(justification, greedy);
  }

  public SequentialBox sequentialBox (Gap gap, Justification justification) {

    return paraboxLayout.sequentialBox(gap, justification);
  }

  public SequentialBox sequentialBox (Gap gap, Justification justification, boolean greedy) {

    return paraboxLayout.sequentialBox(gap, justification, greedy);
  }

  public SequentialBox sequentialBox (double gap, Justification justification) {

    return paraboxLayout.sequentialBox(gap, justification);
  }

  public SequentialBox sequentialBox (double gap, Justification justification, boolean greedy) {

    return paraboxLayout.sequentialBox(gap, justification, greedy);
  }
}
