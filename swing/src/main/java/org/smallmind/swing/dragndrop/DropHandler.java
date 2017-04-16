/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.swing.dragndrop;

import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DropHandler implements DropTargetListener {

  private static final AtomicBoolean DROP_PERMISSION = new AtomicBoolean(false);
  private static final AtomicInteger DROP_ACTIONS = new AtomicInteger(0);

  private DropTarget dropTarget;
  private int actions;

  public DropHandler (Component component, int actions) {

    this(component, actions, true);
  }

  public DropHandler (Component component, int actions, boolean active) {

    this.actions = actions;

    dropTarget = new DropTarget(component, actions, this, active);
  }

  public static boolean hasDropPermission () {

    return DROP_PERMISSION.get();
  }

  public static int getCurrentDropActions () {

    return DROP_ACTIONS.get();
  }

  public abstract boolean canDrop (DropTargetDragEvent dropTargetDragEvent);

  public abstract boolean dropComplete (DropTargetDropEvent dropTargetDropEvent);

  public boolean isActive () {

    return dropTarget.isActive();
  }

  public void setActive (boolean active) {

    dropTarget.setActive(active);
  }

  @Override
  public void dragEnter (DropTargetDragEvent dropTargetDragEvent) {

    DROP_ACTIONS.set(actions);
  }

  @Override
  public void dragOver (DropTargetDragEvent dropTargetDragEvent) {

    DROP_PERMISSION.set(canDrop(dropTargetDragEvent));
  }

  @Override
  public void dropActionChanged (DropTargetDragEvent dropTargetDragEvent) {

  }

  @Override
  public void dragExit (DropTargetEvent dropTargetEvent) {

  }

  @Override
  public void drop (DropTargetDropEvent dropTargetDropEvent) {

    if ((dropTargetDropEvent.getDropAction() & actions) != 0) {
      dropTargetDropEvent.acceptDrop(dropTargetDropEvent.getDropAction());
      dropTargetDropEvent.getDropTargetContext().dropComplete(dropComplete(dropTargetDropEvent));
    }
    else {
      dropTargetDropEvent.rejectDrop();
    }
  }
}
