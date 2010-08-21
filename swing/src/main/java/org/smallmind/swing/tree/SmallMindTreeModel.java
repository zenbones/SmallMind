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
