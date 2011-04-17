/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing.slider;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

public class MultiThumbSlider extends JComponent implements MouseMotionListener, MouseListener {

  private static final ImageIcon HORIZONTAL_THUMB_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/thumb_16.png"));

  public static final int HORIZONTAL = SwingConstants.HORIZONTAL;
  public static final int VERTICAL = SwingConstants.VERTICAL;

  private MultiThumbModel model = new DefaultMultiThumbModel();
  private Integer selectedThumbIndex;
  private int selectedThumbOffset;
  private int orientation = HORIZONTAL;

  public MultiThumbSlider () {

    this(HORIZONTAL);
  }

  public MultiThumbSlider (int orientation) {

    this.orientation = orientation;

    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public MultiThumbModel getModel () {

    return model;
  }

  public void setModel (MultiThumbModel model) {

    this.model = model;
  }

  public void setMinimumValue (int minimumValue) {

    model.setMaximumValue(minimumValue);
  }

  public int getMinimumValue () {

    return model.getMinimumValue();
  }

  public void setMaximumValue (int maximumValue) {

    model.setMaximumValue(maximumValue);
  }

  public int getMaximumValue () {

    return model.getMaximumValue();
  }

  public void addThumb (int thumbValue) {

    model.addThumb(thumbValue);
  }

  public void removeThumb (int thumbIndex) {

    model.removeThumb(thumbIndex);
  }

  public int getTrackLeftEdge () {

    return 7;
  }

  public int getTrackRightEdge () {

    return ((orientation == HORIZONTAL) ? getWidth() : getHeight()) - 7;
  }

  public Dimension getPreferredSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(0, 20);
    }
    else {
      return new Dimension(20, 0);
    }
  }

  public Dimension getMaximumSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(Short.MAX_VALUE, 20);
    }
    else {
      return new Dimension(20, Short.MAX_VALUE);
    }
  }

  @Override
  public void mouseClicked (MouseEvent mouseEvent) {

  }

  @Override
  public void mousePressed (MouseEvent mouseEvent) {

    int pressedThumbIndex;

    if ((orientation == HORIZONTAL) && (mouseEvent.getY() < 16)) {
      if ((pressedThumbIndex = getThumbIndexForPosition(mouseEvent.getX())) >= 0) {
        selectedThumbIndex = pressedThumbIndex;
        selectedThumbOffset = mouseEvent.getX() - (getTrackLeftEdge() + positionForValue(model.getThumbValue(selectedThumbIndex)));
      }
      else {
        selectedThumbIndex = null;
      }
    }
    else if (mouseEvent.getX() < 16) {
      if ((pressedThumbIndex = getThumbIndexForPosition(mouseEvent.getY())) >= 0) {
        selectedThumbIndex = pressedThumbIndex;
        selectedThumbOffset = mouseEvent.getY() - (getTrackLeftEdge() + positionForValue(model.getThumbValue(selectedThumbIndex)));
      }
      else {
        selectedThumbIndex = null;
      }
    }
  }

  @Override
  public void mouseReleased (MouseEvent mouseEvent) {

    selectedThumbIndex = null;
  }

  @Override
  public void mouseEntered (MouseEvent mouseEvent) {

  }

  @Override
  public void mouseExited (MouseEvent mouseEvent) {

  }

  @Override
  public void mouseDragged (MouseEvent mouseEvent) {

    if (selectedThumbIndex != null) {

      int proposedPosition = ((orientation == HORIZONTAL) ? mouseEvent.getX() : mouseEvent.getY()) - selectedThumbOffset - getTrackLeftEdge();
      int proposedValue = valueForPosition(proposedPosition);
      int currentValue = model.getThumbValue(selectedThumbIndex);

      if (proposedValue != currentValue) {

        int minimumValue = getMinimumValue();
        int maximumValue = getMaximumValue();
        int thumbValue;
        int thumbPosition;

        for (int index = 0; index < model.getThumbCount(); index++) {
          if (index != selectedThumbIndex) {
            thumbValue = model.getThumbValue(index);
            thumbPosition = positionForValue(thumbValue);

            if (thumbValue < currentValue) {
              if (valueForPosition(thumbPosition + 15) > minimumValue) {
                minimumValue = valueForPosition(thumbPosition + 15);
                while (positionForValue(minimumValue) < thumbPosition + 15) {
                  minimumValue++;
                }
              }
            }
            if (thumbValue > currentValue) {
              if (valueForPosition(thumbPosition - 15) < maximumValue) {
                maximumValue = valueForPosition(thumbPosition - 15);
                while (positionForValue(maximumValue) > thumbPosition - 15) {
                  maximumValue--;
                }
              }
            }
          }
        }

        if (proposedValue < minimumValue) {
          proposedValue = minimumValue;
        }
        if (proposedValue > maximumValue) {
          proposedValue = maximumValue;
        }

        model.moveThumb(selectedThumbIndex, proposedValue);
        repaint();
      }
    }
  }

  @Override
  public void mouseMoved (MouseEvent mouseEvent) {

  }

  private int getThumbIndexForPosition (int position) {

    int thumbPosition;

    for (int index = 0; index < model.getThumbCount(); index++) {
      thumbPosition = positionForValue(model.getThumbValue(index));
      if ((position >= getTrackLeftEdge() + thumbPosition - 7) && (position <= getTrackLeftEdge() + thumbPosition + 7)) {

        return index;
      }
    }

    return -1;
  }

  public Integer valueForPosition (int position) {

    if (position <= getTrackLeftEdge()) {
      return model.getMinimumValue();
    }
    else if (position >= getTrackRightEdge()) {
      return model.getMaximumValue();
    }
    else {
      return (int)((model.getMaximumValue() - model.getMinimumValue()) * (position / ((double)(getTrackRightEdge() - getTrackLeftEdge()))));
    }
  }

  public int positionForValue (int value) {

    if (orientation == HORIZONTAL) {
      return (int)((getTrackRightEdge() - getTrackLeftEdge()) * (value - getMinimumValue()) / ((double)(getMaximumValue() - getMinimumValue())));
    }
    else {
      return (int)((getTrackRightEdge() - getTrackLeftEdge()) * (value - getMinimumValue()) / ((double)(getMaximumValue() - getMinimumValue())));
    }
  }

  public synchronized void paint (Graphics g) {

    paintTrack(g);
    paintThumb(g);
    paintBorder(g);
  }

  private void paintThumb (Graphics g) {

    for (int index = 0; index < model.getThumbCount(); index++) {
      if (orientation == HORIZONTAL) {
        g.drawImage(HORIZONTAL_THUMB_ICON.getImage(), getTrackLeftEdge() + positionForValue(model.getThumbValue(index)) - 7, 0, null);
      }
      else {
        g.drawImage(HORIZONTAL_THUMB_ICON.getImage(), 0, getTrackLeftEdge() + positionForValue(model.getThumbValue(index)) - 7, null);
      }
    }
  }

  private void paintTrack (Graphics g) {

    if (orientation == HORIZONTAL) {
      g.setColor(SystemColor.controlDkShadow);
      g.drawRect(getTrackLeftEdge(), 5, getTrackRightEdge() - getTrackLeftEdge() - 1, 5);
      g.setColor(SystemColor.controlShadow);
      g.drawLine(getTrackLeftEdge() + 1, 6, getTrackRightEdge() - 2, 6);
    }
    else {
      g.setColor(SystemColor.controlDkShadow);
      g.drawRect(5, getTrackLeftEdge(), 5, getTrackRightEdge() - getTrackLeftEdge() - 1);
      g.setColor(SystemColor.controlShadow);
      g.drawLine(6, getTrackLeftEdge() + 1, 6, getTrackRightEdge() - 2);
    }
  }
}
