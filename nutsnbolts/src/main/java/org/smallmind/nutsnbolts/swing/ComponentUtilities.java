package org.smallmind.nutsnbolts.swing;

import java.awt.Dimension;
import javax.swing.JComponent;

public class ComponentUtilities {

   public static void setWidth (JComponent component, int width) {

      component.setSize(new Dimension(width, component.getHeight()));
   }

   public static void setHeight (JComponent component, int height) {

      component.setSize(new Dimension(component.getWidth(), height));
   }

   public static int getPreferredWidth (JComponent component) {

      return (int)component.getPreferredSize().getWidth();
   }

   public static void setPreferredWidth (JComponent component, int width) {

      component.setPreferredSize(new Dimension(width, (int)component.getPreferredSize().getHeight()));
   }

   public static void setMinimumWidth (JComponent component, int width) {

      component.setMinimumSize(new Dimension(width, (int)component.getMinimumSize().getHeight()));
   }

   public static void setMaximumWidth (JComponent component, int width) {

      component.setMaximumSize(new Dimension(width, (int)component.getMaximumSize().getHeight()));
   }

   public static int getPreferredHeight (JComponent component) {

      return (int)component.getPreferredSize().getHeight();
   }

   public static void setPreferredHeight (JComponent component, int height) {

      component.setPreferredSize(new Dimension((int)component.getPreferredSize().getWidth(), height));
   }

   public static void setMinimumHeight (JComponent component, int height) {

      component.setMinimumSize(new Dimension((int)component.getMinimumSize().getWidth(), height));
   }

   public static void setMaximumHeight (JComponent component, int height) {

      component.setMaximumSize(new Dimension((int)component.getMaximumSize().getWidth(), height));
   }

}
