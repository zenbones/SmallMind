package org.smallmind.swing.label;

import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JLabel;

public class PlainLabel extends JLabel {

   public PlainLabel () {

      super();
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public PlainLabel (Icon icon) {

      super(icon);
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public PlainLabel (Icon icon, int horizontalAlignment) {

      super(icon, horizontalAlignment);
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public PlainLabel (String text) {

      super(text);
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public PlainLabel (String text, int horizontalAlignment) {

      super(text, horizontalAlignment);
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public PlainLabel (String text, Icon icon, int horizontalAlignment) {

      super(text, icon, horizontalAlignment);
      setFont(getFont().deriveFont(Font.PLAIN));
   }

   public synchronized void setFontStyle (int style) {

      setFont(getFont().deriveFont(style));
   }

   public synchronized void setFontSize (float size) {

      setFont(getFont().deriveFont(size));
   }

}
