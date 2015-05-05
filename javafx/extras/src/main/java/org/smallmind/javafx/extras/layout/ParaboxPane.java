/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.javafx.extras.layout;

import java.util.List;
import javafx.geometry.Insets;
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
import org.smallmind.nutsnbolts.layout.Perimeter;
import org.smallmind.nutsnbolts.layout.SerialBox;

public class ParaboxPane extends Region implements ParaboxContainer<Node> {

  private static final ParaboxPlatform PLATFORM = new JavaFxParaboxPlatform();

  private final ParaboxLayout paraboxLayout;

  public ParaboxPane () {

    paraboxLayout = new ParaboxLayout(this);
  }

  public ParaboxPane (Insets insets) {

    paraboxLayout = new ParaboxLayout(new Perimeter(insets.getTop(), insets.getLeft(), insets.getBottom(), insets.getRight()), this);
  }

  @Override
  public ParaboxPlatform getPlatform () {

    return PLATFORM;
  }

  public void removeAll () {

    paraboxLayout.removeAll();
  }

  public void remove (Node node) {

    paraboxLayout.remove(node);
  }

  @Override
  protected double computeMinWidth (double v) {

    return paraboxLayout.calculateMinimumWidth();
  }

  @Override
  protected double computeMinHeight (double v) {

    return paraboxLayout.calculateMinimumHeight();
  }

  @Override
  protected double computePrefWidth (double v) {

    return paraboxLayout.calculatePreferredWidth();
  }

  @Override
  protected double computePrefHeight (double v) {

    return paraboxLayout.calculatePreferredHeight();
  }

  @Override
  protected double computeMaxWidth (double v) {

    return Double.MAX_VALUE;
  }

  @Override
  protected double computeMaxHeight (double v) {

    return Double.MAX_VALUE;
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

  @Override
  public void nativelyRemoveComponent (Node component) {

    getChildren().remove(component);
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

  public SerialBox serialBox () {

    return paraboxLayout.serialBox();
  }

  public SerialBox serialBox (boolean greedy) {

    return paraboxLayout.serialBox(greedy);
  }

  public SerialBox serialBox (Gap gap) {

    return paraboxLayout.serialBox(gap);
  }

  public SerialBox serialBox (Gap gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  public SerialBox serialBox (double gap) {

    return paraboxLayout.serialBox(gap);
  }

  public SerialBox serialBox (double gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  public SerialBox serialBox (Justification justification) {

    return paraboxLayout.serialBox(justification);
  }

  public SerialBox serialBox (Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(justification, greedy);
  }

  public SerialBox serialBox (Gap gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }

  public SerialBox serialBox (double gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }
}
