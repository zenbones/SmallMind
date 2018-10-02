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
package org.smallmind.swing.slider;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Dictionary;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class MultiThumbSlider extends JComponent implements MouseMotionListener, MouseListener, ThumbListener {

  private static final ImageIcon HORIZONTAL_THUMB_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/horizontal_thumb_16.png"));
  private static final ImageIcon VERTICAL_THUMB_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/vertical_thumb_16.png"));

  public static final int HORIZONTAL = SwingConstants.HORIZONTAL;
  public static final int VERTICAL = SwingConstants.VERTICAL;

  private MultiThumbModel model;
  private Dictionary<Integer, String> labelDictionary;
  private Integer selectedThumbIndex;
  private boolean paintTrack = true;
  private boolean paintTickMarks = true;
  private boolean paintLabels = true;
  private int selectedThumbOffset;
  private int orientation = HORIZONTAL;
  private int leftSideAdjustment;
  private int rightSideAdjustment;
  private int majorTickSpacing = 0;
  private int minorTickSpacing = 0;

  public MultiThumbSlider () {

    this(HORIZONTAL, new DefaultMultiThumbModel());
  }

  public MultiThumbSlider (int orientation) {

    this(orientation, new DefaultMultiThumbModel());
  }

  public MultiThumbSlider (MultiThumbModel model) {

    this(HORIZONTAL, model);
  }

  public MultiThumbSlider (int orientation, MultiThumbModel model) {

    this.orientation = orientation;
    this.model = model;

    setFont(new JLabel().getFont().deriveFont(Font.BOLD));
    setDoubleBuffered(true);

    model.addThumbListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public synchronized MultiThumbModel getModel () {

    return model;
  }

  public synchronized void setModel (MultiThumbModel model) {

    this.model.removeThumbListener(this);
    this.model = model;
    this.model.addThumbListener(this);
  }

  public synchronized int getOrientation () {

    return orientation;
  }

  public synchronized void setOrientation (int orientation) {

    this.orientation = orientation;
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

  public synchronized boolean isPaintTrack () {

    return paintTrack;
  }

  public synchronized void setPaintTrack (boolean paintTrack) {

    this.paintTrack = paintTrack;
  }

  public synchronized boolean isPaintTickMarks () {

    return paintTickMarks;
  }

  public synchronized void setPaintTickMarks (boolean paintTickMarks) {

    this.paintTickMarks = paintTickMarks;
  }

  public synchronized boolean isPaintLabels () {

    return paintLabels;
  }

  public synchronized void setPaintLabels (boolean paintLabels) {

    this.paintLabels = paintLabels;
  }

  public synchronized int getMinorTickSpacing () {

    return minorTickSpacing;
  }

  public synchronized void setMinorTickSpacing (int minorTickSpacing) {

    this.minorTickSpacing = minorTickSpacing;
  }

  public synchronized int getMajorTickSpacing () {

    return majorTickSpacing;
  }

  public synchronized void setMajorTickSpacing (int majorTickSpacing) {

    this.majorTickSpacing = majorTickSpacing;
  }

  public synchronized Dictionary<Integer, String> getLabelDictionary () {

    return labelDictionary;
  }

  public synchronized void setLabelDictionary (Dictionary<Integer, String> labelDictionary) {

    this.labelDictionary = labelDictionary;
  }

  public int getThumbCount () {

    return model.getThumbCount();
  }

  public int[] getThumbValues () {

    return model.getThumbValues();
  }

  public int getThumbValue (int thumbIndex) {

    return model.getThumbValue(thumbIndex);
  }

  public void addThumb (int thumbValue) {

    model.addThumb(thumbValue);
  }

  public void removeThumb (int thumbIndex) {

    model.removeThumb(thumbIndex);
  }

  public int getTrackLeftEdge () {

    return 7 - leftSideAdjustment;
  }

  public int getTrackRightEdge () {

    return ((orientation == HORIZONTAL) ? getWidth() : getHeight()) - (7 + rightSideAdjustment);
  }

  public Dimension getPreferredSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(0, 16 + ((paintTickMarks) ? 11 : 0) + ((paintLabels) ? getFontMetrics(getFont()).getAscent() + 1 : 0));
    }
    else {
      return new Dimension(16 + ((paintTickMarks) ? 11 : 0) + ((paintLabels) ? getMaxLabelWidth() + 4 : 0), 0);
    }
  }

  public Dimension getMaximumSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(Short.MAX_VALUE, 16 + ((paintTickMarks) ? 11 : 0) + ((paintLabels) ? getFontMetrics(getFont()).getAscent() + 1 : 0));
    }
    else {
      return new Dimension(16 + ((paintTickMarks) ? 11 : 0) + ((paintLabels) ? getMaxLabelWidth() + 4 : 0), Short.MAX_VALUE);
    }
  }

  private int getMaxLabelWidth () {

    FontMetrics fontMetrics = getFontMetrics(getFont());
    String label;
    int maxWidth = 0;
    int width;

    if (majorTickSpacing > 0) {
      for (int mark = getMinimumValue(); mark <= getMaximumValue(); mark += majorTickSpacing) {
        label = (getLabelDictionary() != null) ? getLabelDictionary().get(mark) : String.valueOf(mark);
        if ((width = fontMetrics.stringWidth(label)) > maxWidth) {
          maxWidth = width;
        }
      }
    }

    return maxWidth;
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
        selectedThumbOffset = (getTrackRightEdge() - positionForValue(model.getThumbValue(selectedThumbIndex))) - mouseEvent.getY();
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

      int proposedPosition;

      if (orientation == HORIZONTAL) {
        proposedPosition = mouseEvent.getX() - selectedThumbOffset - getTrackLeftEdge();
      }
      else {
        proposedPosition = getTrackRightEdge() - mouseEvent.getY() - selectedThumbOffset;
      }

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
      }
    }
  }

  @Override
  public void mouseMoved (MouseEvent mouseEvent) {

  }

  @Override
  public void thumbAdded (ThumbEvent thumbEvent) {

    repaint();
  }

  @Override
  public void thumbRemoved (ThumbEvent thumbEvent) {

    repaint();
  }

  @Override
  public void thumbMoved (ThumbEvent thumbEvent) {

    repaint();
  }

  private int getThumbIndexForPosition (int position) {

    int thumbPosition;

    for (int index = 0; index < model.getThumbCount(); index++) {
      thumbPosition = positionForValue(model.getThumbValue(index));
      if (orientation == HORIZONTAL) {
        if ((position >= getTrackLeftEdge() + thumbPosition - 7) && (position <= getTrackLeftEdge() + thumbPosition + 7)) {

          return index;
        }
      }
      else {
        if ((position >= getTrackRightEdge() - thumbPosition - 7) && (position <= getTrackRightEdge() - thumbPosition + 7)) {

          return index;
        }
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

    return (int)((getTrackRightEdge() - getTrackLeftEdge()) * (value - getMinimumValue()) / ((double)(getMaximumValue() - getMinimumValue())));
  }

  public synchronized void paint (Graphics g) {

    leftSideAdjustment = 0;
    rightSideAdjustment = 0;

    g.setFont(getFont());
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (paintLabels && (majorTickSpacing > 0) && (orientation == HORIZONTAL)) {

      String label;
      int width = getWidth();
      int textPosition;
      int labelWidth;
      int leftOverrun;
      int rightOverrun;

      for (int mark = getMinimumValue(); mark <= getMaximumValue(); mark += majorTickSpacing) {
        label = (getLabelDictionary() != null) ? getLabelDictionary().get(mark) : String.valueOf(mark);
        textPosition = (int)((width - 14) * (mark - getMinimumValue()) / ((double)(getMaximumValue() - getMinimumValue())));
        labelWidth = g.getFontMetrics().stringWidth(label);

        if (((leftOverrun = (textPosition + 7) - (labelWidth / 2)) < 0) && (leftOverrun < leftSideAdjustment)) {
          leftSideAdjustment = leftOverrun;
        }
        if (((rightOverrun = (textPosition + 7) + (labelWidth / 2) - width) > 0) && (rightOverrun > rightSideAdjustment)) {
          rightSideAdjustment = rightOverrun;
        }
      }
    }

    if (paintTrack) {
      paintTrack(g);
    }
    if (paintTickMarks) {
      paintTickMarks(g);
    }
    if (paintLabels) {
      paintLabels(g);
    }
    paintThumb(g);
    paintBorder(g);
  }

  private void paintThumb (Graphics g) {

    for (int index = 0; index < model.getThumbCount(); index++) {
      if (orientation == HORIZONTAL) {
        g.drawImage(HORIZONTAL_THUMB_ICON.getImage(), getTrackLeftEdge() + positionForValue(model.getThumbValue(index)) - 7, 0, null);
      }
      else {
        g.drawImage(VERTICAL_THUMB_ICON.getImage(), 0, getTrackRightEdge() - positionForValue(model.getThumbValue(index)) - 7, null);
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

  private void paintTickMarks (Graphics g) {

    if (minorTickSpacing > 0) {
      g.setColor(SystemColor.controlShadow);
      for (int mark = getMinimumValue(); mark <= getMaximumValue(); mark += minorTickSpacing) {
        if (orientation == HORIZONTAL) {
          g.drawLine(getTrackLeftEdge() + positionForValue(mark), 20, getTrackLeftEdge() + positionForValue(mark), 23);
        }
        else {
          g.drawLine(20, getTrackRightEdge() - positionForValue(mark), 23, getTrackRightEdge() - positionForValue(mark));
        }
      }
    }
    if (majorTickSpacing > 0) {
      g.setColor(SystemColor.controlShadow);
      for (int mark = getMinimumValue(); mark <= getMaximumValue(); mark += majorTickSpacing) {
        if (orientation == HORIZONTAL) {
          g.drawLine(getTrackLeftEdge() + positionForValue(mark), 20, getTrackLeftEdge() + positionForValue(mark), 26);
        }
        else {
          g.drawLine(20, getTrackRightEdge() - positionForValue(mark), 26, getTrackRightEdge() - positionForValue(mark));
        }
      }
    }
  }

  private void paintLabels (Graphics g) {

    FontMetrics fontMetrics = getFontMetrics(getFont());
    String label;

    if (majorTickSpacing > 0) {
      g.setColor(SystemColor.controlText);
      for (int mark = getMinimumValue(); mark <= getMaximumValue(); mark += majorTickSpacing) {
        label = (getLabelDictionary() != null) ? getLabelDictionary().get(mark) : String.valueOf(mark);
        if (orientation == HORIZONTAL) {
          g.drawString(label, getTrackLeftEdge() + positionForValue(mark) - (fontMetrics.stringWidth(label) / 2), (paintTickMarks) ? 28 + fontMetrics.getAscent() : 17 + fontMetrics.getAscent());
        }
        else {
          g.drawString(label, (paintTickMarks) ? 31 : 20, getTrackRightEdge() - positionForValue(mark) + (fontMetrics.getAscent() / 2));
        }
      }
    }
  }
}
