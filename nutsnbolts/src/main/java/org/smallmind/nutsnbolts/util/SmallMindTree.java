package org.smallmind.nutsnbolts.util;

public class SmallMindTree {

   private SmallMindTreeNode root;

   public SmallMindTree (SmallMindTreeNode root) {

      this.root = root;
   }

   public Object getRoot () {

      return root;
   }

   public void setRoot (SmallMindTreeNode root) {

      this.root = root;
   }

}
