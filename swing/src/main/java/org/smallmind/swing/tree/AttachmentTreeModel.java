/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.swing.tree;

import java.util.Iterator;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class AttachmentTreeModel extends Tree implements TreeModel {

  private final WeakEventListenerList<TreeModelListener> listenerList;

  public AttachmentTreeModel (AttachmentTreeNode root) {

    super(root);
    listenerList = new WeakEventListenerList<TreeModelListener>();
  }

  public synchronized void addTreeModelListener (TreeModelListener treeModelListener) {

    listenerList.addListener(treeModelListener);
  }

  public synchronized void removeTreeModelListener (TreeModelListener treeModelListener) {

    listenerList.removeListener(treeModelListener);
  }

  public Object getChild (Object parent, int childIndex) {

    return ((AttachmentTreeNode)parent).getChildAt(childIndex);
  }

  public int getChildCount (Object parent) {

    return ((AttachmentTreeNode)parent).getChildCount();
  }

  public int getIndexOfChild (Object parent, Object child) {

    return ((AttachmentTreeNode)parent).getIndex((AttachmentTreeNode)child);
  }

  public boolean isLeaf (Object node) {

    return ((AttachmentTreeNode)node).isLeaf();
  }

  public void valueForPathChanged (TreePath path, Object newValue) {

    AttachmentTreeNode lastNode = (AttachmentTreeNode)path.getLastPathComponent();
    Object[] children = {};
    int[] childIndices = {};

    if (!lastNode.getAttachment().equals(newValue)) {
      lastNode.setUserObject(newValue);
      fireTreeNodesChanged(this, path.getPath(), childIndices, children);
    }
  }

  public void fireTreeNodesChanged (Object source, Object[] path, int[] childIndices, Object[] children) {

    TreeModelEvent treeModelEvent;
    Iterator<TreeModelListener> listenerIter = listenerList.getListeners();

    treeModelEvent = new TreeModelEvent(source, path, childIndices, children);
    while (listenerIter.hasNext()) {
      listenerIter.next().treeNodesChanged(treeModelEvent);
    }
  }

  public void fireTreeStructureChanged (Object source, Object[] path, int[] childIndices, Object[] children) {

    TreeModelEvent treeModelEvent;
    Iterator<TreeModelListener> listenerIter = listenerList.getListeners();

    treeModelEvent = new TreeModelEvent(source, path, childIndices, children);
    while (listenerIter.hasNext()) {
      listenerIter.next().treeStructureChanged(treeModelEvent);
    }
  }

  public boolean isValidPath (TreePath path) {

    Object[] pathArray = path.getPath();
    int count;

    for (count = 0; count < pathArray.length; count++) {
      if (count == 0) {
        if (!(getRoot()).equals(pathArray[0])) {
          return false;
        }
      } else {
        if (((AttachmentTreeNode)pathArray[count - 1]).getIndex((AttachmentTreeNode)pathArray[count]) < 0) {
          return false;
        }
      }
    }
    return true;
  }
}
