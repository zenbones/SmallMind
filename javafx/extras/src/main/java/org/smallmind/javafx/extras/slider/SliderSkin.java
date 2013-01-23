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
package org.smallmind.javafx.extras.slider;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import com.sun.javafx.scene.control.skin.SkinBase;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.chart.ValueAxis;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public class SliderSkin extends SkinBase<Slider, SliderBehavior> {

  private static final double AXIS_GAP = 2;
  private final StackPane track;
  private final StackPane trackOverlay;
  private final StackPane thumb;
  private SliderAxis axis;
  private Point2D dragStart;
  private double preDragThumbPos;
  private double trackLength;

  public SliderSkin (Slider slider) {

    super(slider, new SliderBehavior(slider));

    getStylesheets().add(Slider.class.getResource("Slider.css").toExternalForm());

    track = new StackPane();
    track.getStyleClass().setAll("track");

    trackOverlay = new StackPane() {

      // This stops the overlay from showing briefly at full track height

      @Override
      protected double computePrefWidth (double v) {

        return 0;
      }

      @Override
      protected double computePrefHeight (double v) {

        return 0;
      }
    };

    trackOverlay.getStyleClass().setAll("track-overlay");
    trackOverlay.setMouseTransparent(true);

    thumb = new StackPane();
    thumb.getStyleClass().setAll("thumb");

    getChildren().clear();
    getChildren().addAll(track, trackOverlay, thumb);

    if (slider.isShowTickMarks() || slider.isShowTickLabels()) {
      getChildren().add(2, axis = new SliderAxis());
    }

    track.setOnMousePressed(new EventHandler<MouseEvent>() {

      @Override
      public void handle (MouseEvent me) {

        if (!thumb.isPressed()) {
          if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
            getBehavior().trackPress(me, (me.getX() / trackLength));
          }

          else getBehavior().trackPress(me, (me.getY() / trackLength));
        }
      }
    });

    track.setOnMouseReleased(new EventHandler<MouseEvent>() {

      @Override
      public void handle (MouseEvent me) {
        //Nothing being done with the second param in sliderBehavior
        //So, passing a dummy value
        getBehavior().trackRelease(me, 0.0f);
      }
    });

    thumb.setOnMousePressed(new EventHandler<MouseEvent>() {

      @Override
      public void handle (MouseEvent me) {

        getBehavior().thumbPressed(me, 0.0f);
        dragStart = thumb.localToParent(me.getX(), me.getY());
        preDragThumbPos = (getSkinnable().getValue() - getSkinnable().getMin()) / (getSkinnable().getMax() - getSkinnable().getMin());
      }
    });

    thumb.setOnMouseReleased(new EventHandler<MouseEvent>() {

      @Override
      public void handle (MouseEvent me) {

        getBehavior().thumbReleased(me);
      }
    });

    thumb.setOnMouseDragged(new EventHandler<MouseEvent>() {

      @Override
      public void handle (MouseEvent me) {

        Point2D cur = thumb.localToParent(me.getX(), me.getY());
        double dragPos = (getSkinnable().getOrientation() == Orientation.HORIZONTAL) ? cur.getX() - dragStart.getX() : -(cur.getY() - dragStart.getY());
        getBehavior().thumbDragged(me, preDragThumbPos + dragPos / trackLength);
      }
    });

    requestLayout();

    registerChangeListener(slider.minProperty(), "MIN");
    registerChangeListener(slider.maxProperty(), "MAX");
    registerChangeListener(slider.valueProperty(), "VALUE");
    registerChangeListener(slider.orientationProperty(), "ORIENTATION");
    registerChangeListener(slider.showTickMarksProperty(), "SHOW_TICK_MARKS");
    registerChangeListener(slider.showTickLabelsProperty(), "SHOW_TICK_LABELS");
    registerChangeListener(slider.majorTickUnitProperty(), "MAJOR_TICK_UNIT");
    registerChangeListener(slider.minorTickCountProperty(), "MINOR_TICK_COUNT");
    registerChangeListener(slider.labelFormatterProperty(), "LABEL_FORMATTER");
  }

  @Override
  protected void handleControlPropertyChanged (String p) {

    super.handleControlPropertyChanged(p);
    if ("ORIENTATION".equals(p)) {
      if (axis != null) {

        Orientation sliderOrientation = getSkinnable().getOrientation();

        if ((sliderOrientation == null) || sliderOrientation.equals(Orientation.VERTICAL)) {
          axis.setSide(Side.RIGHT);
        }
        else {
          axis.setSide(Side.BOTTOM);
          axis.setTickLabelRotation(90);
        }
      }
    }
    else if ("MIN".equals(p)) {
      if (axis != null) {
        // Should be this - axis.setLowerBound(getSkinnable().getMin()); - but axis is bugged and will label incorrectly
        getChildren().remove(axis);
        getChildren().add(2, axis = new SliderAxis());
      }
    }
    else if ("MAX".equals(p)) {
      if (axis != null) {
        // Should be this - axis.setUpperBound(getSkinnable().getMax()); - but axis is bugged and will label incorrectly
        getChildren().remove(axis);
        getChildren().add(2, axis = new SliderAxis());
      }
    }
    else if ("SHOW_TICK_MARKS".equals(p) || "SHOW_TICK_LABELS".equals(p)) {
      if (getSkinnable().isShowTickMarks() || getSkinnable().isShowTickLabels()) {
        if (axis == null) {
          getChildren().add(2, axis = new SliderAxis());
        }
      }
      else if (axis != null) {
        getChildren().remove(axis);
        axis = null;
      }
    }
    else if ("MAJOR_TICK_UNIT".equals(p)) {
      if (axis != null) {
        axis.setMinorTickCount(getSkinnable().getMinorTickCount());
      }
    }
    else if ("MINOR_TICK_COUNT".equals(p)) {
      if (axis != null) {
        axis.setMinorTickCount(getSkinnable().getMinorTickCount());
      }
    }
    else if ("LABEL_FORMATTER".equals(p)) {
      if (axis != null) {
        axis.setTickLabelFormatter(getSkinnable().getLabelFormatter());
      }
    }

    requestLayout();
  }

  @Override
  protected void layoutChildren () {

    double thumbWidth = thumb.prefWidth(-1);
    double thumbHeight = thumb.prefHeight(-1);
    double x = getInsets().getLeft();
    double y = getInsets().getTop();
    double width = getWidth() - (getInsets().getLeft() + getInsets().getRight());
    double height = getHeight() - (getInsets().getTop() + getInsets().getBottom());
    double trackRadius = 3;
    double sliderValue;
    double sliderMin;
    double sliderMax;
    double axisLength;
    double endCap;
    double offset;

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {

      trackLength = width - (endCap = Math.max(thumbWidth, trackRadius * 2));
      axisLength = trackLength - (2 * trackRadius);
      offset = Math.max(thumbHeight / 2, trackRadius);

      track.resizeRelocate(x + (endCap / 2), y + offset - trackRadius, trackLength, trackRadius * 2);
      thumb.resizeRelocate(x + (endCap / 2) + trackRadius + (((sliderValue = getSkinnable().getValue()) - (sliderMin = getSkinnable().getMin())) / ((sliderMax = getSkinnable().getMax()) - sliderMin) * axisLength) - (thumbWidth / 2), y + offset - (thumbHeight / 2), thumbWidth, thumbHeight);
      trackOverlay.resizeRelocate(x + (endCap / 2), y + offset - trackRadius, ((sliderValue - sliderMin) / (sliderMax - sliderMin)) * trackLength, trackRadius * 2);

      if (axis != null) {
        axis.resizeRelocate(x + (endCap / 2) + trackRadius, y + offset + trackRadius + AXIS_GAP, axisLength, axis.prefHeight(-1));
      }
    }
    else {

      trackLength = height - (endCap = Math.max(thumbHeight, trackRadius * 2));
      axisLength = trackLength - (2 * trackRadius);
      offset = Math.max(thumbWidth / 2, trackRadius);

      track.resizeRelocate(x + offset - trackRadius, y + (endCap / 2), trackRadius * 2, trackLength);
      thumb.resizeRelocate(x + offset - (thumbWidth / 2), y + (endCap / 2) + trackRadius + axisLength - (((sliderValue = getSkinnable().getValue()) - (sliderMin = getSkinnable().getMin())) / ((sliderMax = getSkinnable().getMax()) - sliderMin) * axisLength) - (thumbHeight / 2), thumbWidth, thumbHeight);

      double overlayLength = ((sliderValue - sliderMin) / (sliderMax - sliderMin)) * trackLength;

      trackOverlay.resizeRelocate(x + offset - trackRadius, y + (endCap / 2) + trackLength - overlayLength, trackRadius * 2, overlayLength);

      if (axis != null) {
        axis.resizeRelocate(x + offset + trackRadius + AXIS_GAP, y + (endCap / 2) + trackRadius, axis.prefWidth(-1), axisLength);
      }
    }
  }

  @Override
  protected double computeMinWidth (double height) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      return (getInsets().getLeft() + (thumb.minWidth(-1) * 3) + getInsets().getRight());
    }
    else {
      return (getInsets().getLeft() + thumb.prefWidth(-1) + getInsets().getRight());
    }
  }

  @Override
  protected double computeMinHeight (double width) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      return (getInsets().getTop() + thumb.prefHeight(-1) + getInsets().getBottom());
    }
    else {
      return (getInsets().getTop() + (3 * thumb.prefHeight(-1)) + getInsets().getBottom());
    }
  }

  @Override
  protected double computePrefWidth (double height) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      if (axis != null) {
        return Math.max(getSkinnable().getPrefWidth(), axis.prefWidth(-1));
      }
      else {
        return getSkinnable().getPrefWidth();
      }
    }
    else {
      return (getInsets().getLeft()) + Math.max(thumb.prefWidth(-1), track.prefWidth(-1)) + ((axis != null) ? (AXIS_GAP + axis.prefWidth(-1)) : 0) + getInsets().getRight();
    }
  }

  @Override
  protected double computePrefHeight (double width) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      return getInsets().getTop() + Math.max(thumb.prefHeight(-1), track.prefHeight(-1)) + ((axis != null) ? (AXIS_GAP + axis.prefHeight(-1)) : 0) + getInsets().getBottom();
    }
    else {
      if (axis != null) {
        return Math.max(getSkinnable().getPrefHeight(), axis.prefHeight(-1));
      }
      else {
        return getSkinnable().getPrefHeight();
      }
    }
  }

  @Override
  protected double computeMaxWidth (double height) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      return Double.MAX_VALUE;
    }
    else {
      return getSkinnable().prefWidth(-1);
    }
  }

  @Override
  protected double computeMaxHeight (double width) {

    if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
      return getSkinnable().prefHeight(width);
    }
    else {
      return Double.MAX_VALUE;
    }
  }

  public class SliderAxis extends ValueAxis<Double> {

    public SliderAxis () {

      Orientation sliderOrientation = getSkinnable().getOrientation();

      if ((sliderOrientation == null) || sliderOrientation.equals(Orientation.VERTICAL)) {
        setSide(Side.RIGHT);
      }
      else {
        setSide(Side.BOTTOM);
        setTickLabelRotation(90);
      }

      setLowerBound(getSkinnable().getMin());
      setUpperBound(getSkinnable().getMax());
      setMinorTickCount(getSkinnable().getMinorTickCount());
      setTickLabelFormatter(getSkinnable().getLabelFormatter());

      setAutoRanging(false);
    }

    @Override
    public void resizeRelocate (double x, double y, double width, double height) {

      super.resizeRelocate(x, y, width, height);
      layoutChildren();
    }

    @Override
    protected void setRange (Object range, boolean animate) {

      throw new UnsupportedOperationException();
    }

    @Override
    protected Object getRange () {

      return null;
    }

    @Override
    protected List<Double> calculateTickValues (double length, Object range) {

      LinkedList<Double> tickList = new LinkedList<Double>();
      double tickMin = getSkinnable().getMin();
      double tickUnit = getSkinnable().getMajorTickUnit();
      double current = (tickMin / tickUnit) * tickUnit;
      double stop = getSkinnable().getMax();

      while (current <= stop) {
        if (current >= tickMin) {
          tickList.add(current);
        }

        current += tickUnit;
      }

      return tickList;
    }

    @Override
    protected List<Double> calculateMinorTickMarks () {

      int minorTickCount;

      if ((minorTickCount = getMinorTickCount()) == 0) {

        return Collections.emptyList();
      }

      LinkedList<Double> tickList = new LinkedList<Double>();
      double tickMin = getSkinnable().getMin();
      double majorTickUnit = getSkinnable().getMajorTickUnit();
      double minorTickUnit = majorTickUnit / (minorTickCount + 1);
      double current = ((tickMin / majorTickUnit) * majorTickUnit) + minorTickUnit;
      double stop = getSkinnable().getMax();
      int index = 0;

      while (current <= stop) {
        if (index++ == minorTickCount) {
          index = 0;
        }
        else {
          if (current >= tickMin) {
            tickList.add(current);
          }
        }

        current += minorTickUnit;
      }

      return tickList;
    }

    @Override
    protected String getTickMarkLabel (Double value) {

      if (!getSkinnable().isShowTickLabels()) {

        return "";
      }

      StringConverter<Double> converter;

      return ((converter = getTickLabelFormatter()) != null) ? converter.toString(value) : String.valueOf(value);
    }
  }
}
