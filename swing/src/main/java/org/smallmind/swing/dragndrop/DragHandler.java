/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.swing.dragndrop;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DragSourceMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

public abstract class DragHandler implements DragSourceListener, DragSourceMotionListener, DragGestureListener {

  private GhostPanel ghostPanel;
  private CursorPair cursorPair;
  private Component component;
  private Point imageOffset = new Point(0, 0);
  private Point ghostPoint = new Point(0, 0);
  private int actions;

  public DragHandler (GhostPanel ghostPanel, Component component, int actions) {

    this.ghostPanel = ghostPanel;
    this.component = component;
    this.actions = actions;

    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(component, actions, this);
  }

  public abstract Transferable getTransferable (DragGestureEvent dragGestureEvent);

  public abstract Icon getDragIcon (DragGestureEvent dragGestureEvent, Point offset);

  public abstract void dragTerminated (int action, boolean success);

  private BufferedImage createImageFromIcon (Icon icon) {

    BufferedImage image;
    Graphics graphics;

    image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
    graphics = image.getGraphics();

    icon.paintIcon(component, graphics, 0, 0);
    graphics.dispose();

    return image;
  }

  @Override
  public void dragGestureRecognized (DragGestureEvent dragGestureEvent) {

    Transferable transferable;
    Icon dragIcon;

    if (((dragGestureEvent.getDragAction() & actions) != 0) && ((transferable = getTransferable(dragGestureEvent)) != null)) {
      cursorPair = new CursorPair(dragGestureEvent.getDragAction());

      if (((dragIcon = getDragIcon(dragGestureEvent, imageOffset)) != null) && DragSource.isDragImageSupported()) {
        dragGestureEvent.startDrag(cursorPair.getNoDropCursor(), createImageFromIcon(dragIcon), imageOffset, transferable, this);
      }
      else {
        if (dragIcon != null) {
          ghostPanel.setImage(createImageFromIcon(dragIcon));
          ghostPanel.setLocation(0, 0);
          ghostPanel.setCursor(cursorPair.getNoDropCursor());
          ghostPanel.setVisible(true);
        }
        dragGestureEvent.startDrag(cursorPair.getNoDropCursor(), transferable, this);
      }

      dragGestureEvent.getDragSource().addDragSourceMotionListener(this);
    }
  }

  @Override
  public void dragMouseMoved (DragSourceDragEvent dragSourceDragEvent) {

    ghostPoint.setLocation(dragSourceDragEvent.getLocation());
    SwingUtilities.convertPointFromScreen(ghostPoint, ghostPanel);
    ghostPoint.translate((int)imageOffset.getX(), (int)imageOffset.getY());
    ghostPanel.setImageLocation(ghostPoint);
    ghostPanel.repaint();
  }

  @Override
  public void dragEnter (DragSourceDragEvent dragSourceDragEvent) {

  }

  @Override
  public void dragOver (DragSourceDragEvent dragSourceDragEvent) {

    Cursor cursor = (((DropHandler.getCurrentDropActions() & dragSourceDragEvent.getUserAction() & actions) != 0) && DropHandler.hasDropPermission()) ? cursorPair.getDropCursor() : cursorPair.getNoDropCursor();

    ghostPanel.setCursor(cursor);
    dragSourceDragEvent.getDragSourceContext().setCursor(cursor);
  }

  @Override
  public void dropActionChanged (DragSourceDragEvent dragSourceDragEvent) {

    cursorPair.setCursors(dragSourceDragEvent.getUserAction());
  }

  @Override
  public void dragExit (DragSourceEvent dragSourceEvent) {

    ghostPanel.setCursor(cursorPair.getNoDropCursor());
    dragSourceEvent.getDragSourceContext().setCursor(cursorPair.getNoDropCursor());
  }

  @Override
  public void dragDropEnd (DragSourceDropEvent dragSourceDropEvent) {

    try {
      dragTerminated(dragSourceDropEvent.getDropAction(), dragSourceDropEvent.getDropSuccess());
    }
    finally {
      ghostPanel.setVisible(false);
      ghostPanel.setImage(null);
      ghostPanel.setImageLocation(null);
      dragSourceDropEvent.getDragSourceContext().getDragSource().removeDragSourceMotionListener(this);
      cursorPair = null;
    }
  }

  private class CursorPair {

    private Cursor dropCursor;
    private Cursor noDropCursor;

    public CursorPair (int action) {

      setCursors(action);
    }

    public synchronized void setCursors (int action) {

      if ((action & DnDConstants.ACTION_LINK) != 0) {
        dropCursor = DragSource.DefaultLinkDrop;
        noDropCursor = DragSource.DefaultLinkNoDrop;
      }
      else if ((action & DnDConstants.ACTION_COPY) != 0) {
        dropCursor = DragSource.DefaultCopyDrop;
        noDropCursor = DragSource.DefaultCopyNoDrop;
      }
      else if ((action & DnDConstants.ACTION_MOVE) != 0) {
        dropCursor = DragSource.DefaultMoveDrop;
        noDropCursor = DragSource.DefaultMoveNoDrop;
      }
      else {
        dropCursor = DragSource.DefaultMoveNoDrop;
        noDropCursor = DragSource.DefaultMoveNoDrop;
      }
    }

    public synchronized Cursor getDropCursor () {

      return dropCursor;
    }

    public synchronized Cursor getNoDropCursor () {

      return noDropCursor;
    }
  }
}
