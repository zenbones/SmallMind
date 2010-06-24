package org.smallmind.nutsnbolts.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class SmallMindTreeNode implements Cloneable, Serializable, MutableTreeNode {

   private SmallMindTreeNode parent;
   private Object userObject;
   protected LinkedList<TreeNode> childList;
   private boolean allowsChildren;

   public SmallMindTreeNode () {

      this(null, true);
   }

   public SmallMindTreeNode (Object userObject) {

      this(userObject, true);
   }

   public SmallMindTreeNode (Object userObject, boolean allowsChildren) {

      parent = null;
      this.userObject = userObject;
      this.allowsChildren = allowsChildren;
      if (allowsChildren) {
         childList = new LinkedList<TreeNode>();
      }
      else {
         childList = null;
      }
   }

   public synchronized void setAllowsChildren (boolean newAllowsChildren) {

      if (allowsChildren != newAllowsChildren) {
         allowsChildren = newAllowsChildren;
         if (allowsChildren) {
            childList = new LinkedList<TreeNode>();
         }
         else {
            childList = null;
         }
      }
   }

   public synchronized boolean getAllowsChildren () {

      return allowsChildren;
   }

   public synchronized Enumeration children () {

      if (!allowsChildren) {
         return null;
      }
      return Collections.enumeration(childList);
   }

   public synchronized TreeNode getChildAt (int childIndex) {

      if (!allowsChildren) {
         return null;
      }
      return childList.get(childIndex);
   }

   public synchronized int getChildCount () {

      if (!allowsChildren) {
         return 0;
      }
      return childList.size();
   }

   public synchronized int getIndex (TreeNode node) {

      if (!allowsChildren) {
         return -1;
      }
      return childList.indexOf(node);
   }

   public synchronized int getUserObjectIndex (Object childUserObject) {

      SmallMindTreeNode childNode;
      int count;

      if (!allowsChildren) {
         return -1;
      }
      for (count = 0; count < childList.size(); count++) {
         childNode = (SmallMindTreeNode)childList.get(count);
         if (childNode.getUserObject() != null) {
            if (childNode.getUserObject().equals(childUserObject)) {
               return count;
            }
         }
      }
      return -1;
   }

   public synchronized TreeNode getParent () {

      return parent;
   }

   public synchronized void setParent (MutableTreeNode parent) {

      this.parent = (SmallMindTreeNode)parent;
   }

   public synchronized Object getUserObject () {

      return userObject;
   }

   public synchronized void setUserObject (Object userObject) {

      this.userObject = userObject;
   }

   public synchronized void sortChildren (Comparator<TreeNode> sort) {

      if (allowsChildren) {
         Collections.sort(childList, sort);
      }
   }

   public synchronized TreeNode getPrevInThread () {

      return parent;
   }

   public synchronized TreeNode getNextInThread () {

      if (!allowsChildren) {
         return null;
      }
      if (childList.size() == 0) {
         return null;
      }
      return childList.get(0);
   }

   public synchronized TreePath getTreePath () {

      ArrayList<TreeNode> pathList = new ArrayList<TreeNode>();
      TreeNode curNode = this;
      Object[] thisNodesPath;

      pathList.add(curNode);
      while (curNode.getParent() != null) {
         curNode = curNode.getParent();
         pathList.add(0, curNode);
      }
      thisNodesPath = pathList.toArray();
      return new TreePath(thisNodesPath);
   }

   public synchronized boolean isLeaf () {

      return (!allowsChildren);
   }

   public synchronized void add (MutableTreeNode child) {

      if (allowsChildren) {
         childList.add(child);
         child.setParent(this);
      }
   }

   public synchronized void insert (MutableTreeNode child, int childIndex) {

      if (allowsChildren) {
         childList.add(childIndex, child);
         child.setParent(this);
      }
   }

   public synchronized void set (MutableTreeNode child, int childIndex) {

      if (allowsChildren) {
         childList.set(childIndex, child);
         child.setParent(this);
      }
   }

   public synchronized void remove (int childIndex) {

      SmallMindTreeNode child;

      if (allowsChildren) {
         child = (SmallMindTreeNode)childList.get(childIndex);
         childList.remove(childIndex);
         child.setParent(null);
      }
   }

   public synchronized void remove (MutableTreeNode child) {

      if (allowsChildren) {
         if (childList.remove(child)) {
            child.setParent(null);
         }
      }
   }

   public void removeFromParent () {

      parent.remove(this);
      setParent(null);
   }

   public String toString () {

      if (userObject != null) {
         return userObject.toString();
      }
      else {
         return "null";
      }
   }

}
