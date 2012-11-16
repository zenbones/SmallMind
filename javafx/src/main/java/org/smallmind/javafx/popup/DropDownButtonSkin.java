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
package org.smallmind.javafx.popup;

import com.sun.javafx.scene.control.behavior.ButtonBehavior;
import com.sun.javafx.scene.control.skin.SkinBase;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class DropDownButtonSkin extends SkinBase<DropDownButton, ButtonBehavior<DropDownButton>> {

  private final StackPane openButton;
  private final Label label;

  public DropDownButtonSkin (DropDownButton dropDownButton) {

    super(dropDownButton, new ButtonBehavior<DropDownButton>(dropDownButton));

    getStylesheets().add(DropDownButton.class.getResource("DropDownButton.css").toExternalForm());
    getStyleClass().add("drop-down-box");

    label = new Label("Foo Bar");
    label.getStyleClass().add("label");
    label.setMnemonicParsing(false);

    openButton = new StackPane();
    openButton.getStyleClass().add("open-button");

    StackPane region = new StackPane();
    region.getStyleClass().add("arrow");
    openButton.getChildren().addAll(region);

    getChildren().setAll(label, openButton);

    requestLayout();
  }

  @Override
  protected void layoutChildren () {

    label.resizeRelocate(getInsets().getLeft(), getInsets().getTop(), label.prefWidth(-1), label.prefHeight(-1));
    openButton.resizeRelocate(getInsets().getLeft() + getWidth() - (openButton.prefWidth(-1) + getInsets().getRight()), getInsets().getTop() + ((getHeight() - openButton.prefHeight(-1)) / 2), openButton.prefWidth(-1), openButton.prefHeight(-1));
  }

  @Override
  protected double computeMinWidth (double height) {

    return computePrefWidth(height);
  }

  @Override
  protected double computeMinHeight (double width) {

    return computePrefHeight(width);
  }

  @Override
  protected double computePrefWidth (double height) {

    return getInsets().getLeft() + label.prefWidth(height) + openButton.prefWidth(-1) + getInsets().getRight();
  }

  @Override
  protected double computePrefHeight (double width) {

    return getInsets().getTop() + label.prefHeight(width) + getInsets().getBottom();
  }

  @Override
  protected double computeMaxWidth (double height) {

    return computePrefWidth(height);
  }

  @Override
  protected double computeMaxHeight (double width) {

    return computePrefHeight(width);
  }
}
