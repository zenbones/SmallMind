package org.smallmind.swing.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.io.Serializable;

public class ListLayout implements LayoutManager, Serializable {

   private int gap;

   public ListLayout () {

      this(0);
   }

   public ListLayout (int gap) {

      this.gap = gap;
   }

   public void addLayoutComponent (String s, Component component) {
   }

   public void removeLayoutComponent (Component component) {
   }

   public Dimension preferredLayoutSize (Container container) {

      Component component;
      Dimension preferredSize;
      int height = 0;
      int width;

      width = container.getWidth() - container.getInsets().left - container.getInsets().right;

      synchronized (container.getTreeLock()) {
         for (int count = 0; count < container.getComponentCount(); count++) {
            component = container.getComponent(count);
            if (component.isVisible()) {
               preferredSize = component.getPreferredSize();
               height += preferredSize.getHeight();
               if (count > 0) {
                  height += gap;
               }
            }
         }

         height += container.getInsets().top + container.getInsets().bottom;

         return new Dimension(width, height);
      }
   }

   public Dimension minimumLayoutSize (Container container) {

      Component component;
      Dimension minSize;
      int height = 0;
      int width;

      width = container.getWidth() - container.getInsets().left - container.getInsets().right;

      synchronized (container.getTreeLock()) {
         for (int count = 0; count < container.getComponentCount(); count++) {
            component = container.getComponent(count);
            if (component.isVisible()) {
               minSize = component.getMinimumSize();
               height += minSize.getHeight();
               if (count > 0) {
                  height += gap;
               }
            }
         }

         height += container.getInsets().top + container.getInsets().bottom;

         return new Dimension(width, height);
      }
   }

   public void layoutContainer (Container container) {

      Component component;
      Dimension preferredSize;
      int width;
      int xPos;
      int yPos;

      width = container.getWidth() - container.getInsets().left - container.getInsets().right;
      xPos = container.getInsets().left;
      yPos = container.getInsets().top;

      synchronized (container.getTreeLock()) {
         for (int count = 0; count < container.getComponentCount(); count++) {
            component = container.getComponent(count);
            if (component.isVisible()) {
               preferredSize = component.getPreferredSize();
               component.setLocation(xPos, yPos);
               component.setSize(width, (int)preferredSize.getHeight());
               yPos += (preferredSize.getHeight() + gap);
            }
         }
      }
   }

}
