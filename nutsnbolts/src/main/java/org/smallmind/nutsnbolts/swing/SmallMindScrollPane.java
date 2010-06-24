package org.smallmind.nutsnbolts.swing;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SmallMindScrollPane extends ExpandedScrollPane {

   private static JPanel UPPER_CORNER_PANEL;
   private static JPanel LOWER_CORNER_PANEL;

   static {

      UPPER_CORNER_PANEL = new JPanel();
      LOWER_CORNER_PANEL = new JPanel();
   }

   public SmallMindScrollPane (Component component) {

      super(component);

      setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      setCorner(JScrollPane.UPPER_RIGHT_CORNER, UPPER_CORNER_PANEL);
      setCorner(JScrollPane.LOWER_RIGHT_CORNER, LOWER_CORNER_PANEL);
   }

}
