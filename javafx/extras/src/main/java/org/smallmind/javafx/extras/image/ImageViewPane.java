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
package org.smallmind.javafx.extras.image;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * A {@link Region} that manages a single {@link ImageView} child and stretches it to fill the
 * pane's bounds on every layout pass. The hosted image view is exposed as an observable property
 * so that it can be swapped at runtime; swapping automatically removes the old view from the
 * scene graph and adds the replacement.
 */
public class ImageViewPane extends Region {

  private final ObjectProperty<ImageView> imageViewProperty = new SimpleObjectProperty<>();

  /**
   * Creates a pane hosting the supplied image view. The view is immediately added to the scene
   * graph as a managed child.
   *
   * @param imageView the image view to display; may be {@code null} to start with an empty pane
   */
  public ImageViewPane (ImageView imageView) {

    imageViewProperty.addListener(new ChangeListener<ImageView>() {

      @Override
      public void changed (ObservableValue<? extends ImageView> observable, ImageView oldImageView, ImageView newImageView) {

        if (oldImageView != null) {
          getChildren().remove(oldImageView);
        }
        if (newImageView != null) {
          getChildren().add(newImageView);
        }
      }
    });
    this.imageViewProperty.set(imageView);
  }

  /**
   * Returns the observable property that holds the currently hosted {@link ImageView}. Setting
   * a new value on this property replaces the displayed image and updates the scene graph.
   *
   * @return the image-view property; never {@code null}
   */
  public ObjectProperty<ImageView> imageViewProperty () {

    return imageViewProperty;
  }

  /**
   * Returns the currently hosted {@link ImageView}.
   *
   * @return the image view, or {@code null} if none has been set
   */
  public ImageView getImageView () {

    return imageViewProperty.get();
  }

  /**
   * Replaces the currently hosted image view. The previous view is removed from the scene graph
   * and the new one is added automatically.
   *
   * @param imageView the new image view to display; may be {@code null} to clear the pane
   */
  public void setImageView (ImageView imageView) {

    this.imageViewProperty.set(imageView);
  }

  /**
   * Resizes and repositions the hosted image view to exactly fill the pane's current bounds,
   * centred horizontally and vertically.
   */
  @Override
  protected void layoutChildren () {

    ImageView imageView = imageViewProperty.get();
    if (imageView != null) {
      imageView.setFitWidth(getWidth());
      imageView.setFitHeight(getHeight());
      layoutInArea(imageView, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    super.layoutChildren();
  }
}
