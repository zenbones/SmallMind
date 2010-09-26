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
package org.smallmind.swing.tree;

import java.util.Iterator;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.smallmind.nutsnbolts.util.SmallMindTree;
import org.smallmind.nutsnbolts.util.SmallMindTreeNode;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class SmallMindTreeModel extends SmallMindTree implements TreeModel {

   private WeakEventListenerList<TreeModelListener> listenerList;

   public SmallMindTreeModel (SmallMindTreeNode root) {

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

      return ((SmallMindTreeNode)parent).getChildAt(childIndex);
   }

   public int getChildCount (Object parent) {

      return ((SmallMindTreeNode)parent).getChildCount();
   }

   public int getIndexOfChild (Object parent, Object child) {

      return ((SmallMindTreeNode)parent).getIndex((SmallMindTreeNode)child);
   }

   public boolean isLeaf (Object node) {

      return ((SmallMindTreeNode)node).isLeaf();
   }

   public void valueForPathChanged (TreePath path, Object newValue) {

      SmallMindTreeNode lastNode = (SmallMindTreeNode)path.getLastPathComponent();
      Object[] children = {};
      int[] childIndices = {};

      if (!lastNode.getUserObject().equals(newValue)) {
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
         }
         else {
            if (((SmallMindTreeNode)pathArray[count - 1]).getIndex((SmallMindTreeNode)pathArray[count]) < 0) {
               return false;
            }
         }
      }
      return true;
   }

}
