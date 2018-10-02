/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

public class ImageViewPane extends Region {

  private ObjectProperty<ImageView> imageViewProperty = new SimpleObjectProperty<>();

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

  public ObjectProperty<ImageView> imageViewProperty () {

    return imageViewProperty;
  }

  public ImageView getImageView () {

    return imageViewProperty.get();
  }

  public void setImageView (ImageView imageView) {

    this.imageViewProperty.set(imageView);
  }

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