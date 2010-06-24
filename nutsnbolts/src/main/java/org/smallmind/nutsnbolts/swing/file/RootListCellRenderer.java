package org.smallmind.nutsnbolts.swing.file;

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class RootListCellRenderer implements ListCellRenderer {

   private static ImageIcon DRIVE;

   private HashMap<File, JLabel> rootLabelMap;

   static {

      DRIVE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/harddisk.png"));
   }

   public RootListCellRenderer () {

      rootLabelMap = new HashMap<File, JLabel>();
   }

   public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      JLabel rootLabel;

      if ((rootLabel = rootLabelMap.get(value)) == null) {
         rootLabel = new JLabel(((File)value).getAbsolutePath(), DRIVE, SwingConstants.LEFT);
         rootLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
         rootLabel.setOpaque(true);
         rootLabelMap.put((File)value, rootLabel);
      }

      if (isSelected) {
         rootLabel.setBackground(UIManager.getDefaults().getColor("textHighlight"));
      }
      else {
         rootLabel.setBackground(UIManager.getDefaults().getColor("control"));
      }

      return rootLabel;
   }

}
