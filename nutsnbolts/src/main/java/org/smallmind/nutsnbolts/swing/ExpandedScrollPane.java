package org.smallmind.nutsnbolts.swing;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

public class ExpandedScrollPane extends JScrollPane {

   protected JViewport columnFooter;

   public ExpandedScrollPane (Component view) {

      super(view);

      setLayout(new ExpandedScrollPaneLayout());
   }

   public JViewport getColumnFooter () {
      return columnFooter;
   }

   public void setColumnFooter (JViewport columnFooter) {
      JViewport old = getColumnFooter();
      this.columnFooter = columnFooter;
      if (columnFooter != null) {
         add(columnFooter, "COLUMN_FOOTER");
      }
      else if (old != null) {
         remove(old);
      }
      firePropertyChange("columnFooter", old, columnFooter);

      revalidate();
      repaint();
   }

   public void setColumnFooterView (Component view) {
      if (getColumnFooter() == null) {
         setColumnFooter(createViewport());
      }
      getColumnFooter().setView(view);
   }

}

