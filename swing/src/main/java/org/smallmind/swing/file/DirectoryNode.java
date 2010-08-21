package org.smallmind.swing.file;

import java.util.Comparator;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import org.smallmind.nutsnbolts.util.SmallMindTreeNode;

public class DirectoryNode extends SmallMindTreeNode {

   private boolean instantiated = false;

   public DirectoryNode (Directory directory) {

      super(directory, directory.hasChildren());
   }

   private synchronized void instantiateChildren () {

      Directory[] childDirectories;

      if (!instantiated) {
         instantiated = true;

         if (getAllowsChildren()) {
            childDirectories = ((Directory)getUserObject()).getChildren();
            for (Directory childDirectory : childDirectories) {
               add(new DirectoryNode(childDirectory));
            }
         }
      }
   }

   public Enumeration children () {
      instantiateChildren();

      return super.children();
   }

   public TreeNode getChildAt (int childIndex) {
      instantiateChildren();

      return super.getChildAt(childIndex);
   }

   public int getChildCount () {
      instantiateChildren();

      return super.getChildCount();
   }

   public int getIndex (TreeNode node) {
      instantiateChildren();

      return super.getIndex(node);
   }

   public void sortChildren (Comparator<TreeNode> sort) {
      instantiateChildren();

      super.sortChildren(sort);
   }

}
