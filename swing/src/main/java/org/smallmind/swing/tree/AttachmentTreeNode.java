/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import jakarta.swing.tree.MutableTreeNode;
import jakarta.swing.tree.TreeNode;
import jakarta.swing.tree.TreePath;

public class AttachmentTreeNode implements Serializable, MutableTreeNode {

  private AttachmentTreeNode parent;
  private Object attachment;
  private boolean allowsChildren;
  protected LinkedList<TreeNode> childList;

  public AttachmentTreeNode () {

    this(null, true);
  }

  public AttachmentTreeNode (Object attachment) {

    this(attachment, true);
  }

  public AttachmentTreeNode (Object attachment, boolean allowsChildren) {

    parent = null;
    this.attachment = attachment;
    this.allowsChildren = allowsChildren;
    if (allowsChildren) {
      childList = new LinkedList<TreeNode>();
    } else {
      childList = null;
    }
  }

  public synchronized boolean getAllowsChildren () {

    return allowsChildren;
  }

  public synchronized void setAllowsChildren (boolean newAllowsChildren) {

    if (allowsChildren != newAllowsChildren) {
      allowsChildren = newAllowsChildren;
      if (allowsChildren) {
        childList = new LinkedList<>();
      } else {
        childList = null;
      }
    }
  }

  public synchronized Enumeration<TreeNode> children () {

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

    AttachmentTreeNode childNode;
    int count;

    if (!allowsChildren) {
      return -1;
    }
    for (count = 0; count < childList.size(); count++) {
      childNode = (AttachmentTreeNode)childList.get(count);
      if (childNode.getAttachment() != null) {
        if (childNode.getAttachment().equals(childUserObject)) {
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

    this.parent = (AttachmentTreeNode)parent;
  }

  public synchronized Object getAttachment () {

    return attachment;
  }

  public synchronized void setUserObject (Object userObject) {

    this.attachment = userObject;
  }

  public synchronized void sortChildren (Comparator<TreeNode> sort) {

    if (allowsChildren) {
      childList.sort(sort);
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

    AttachmentTreeNode child;

    if (allowsChildren) {
      child = (AttachmentTreeNode)childList.get(childIndex);
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

    if (attachment != null) {
      return attachment.toString();
    } else {
      return "null";
    }
  }
}
