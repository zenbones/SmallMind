package org.smallmind.nutsnbolts.swing.file;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.TreeCellRenderer;

public class DirectoryTreeCellRenderer implements TreeCellRenderer {

   private static ImageIcon DRIVE;
   private static ImageIcon FOLDER;
   private static ImageIcon FOLDERS;

   private static Border SELECTED_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getDefaults().getColor("textHighlight").darker()), BorderFactory.createEmptyBorder(1, 1, 1, 1));
   private static Border INVISIBLE_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getDefaults().getColor("text")), BorderFactory.createEmptyBorder(1, 1, 1, 1));

   private HashMap<DirectoryNode, JLabel> directoryLabelMap;

   static {

      DRIVE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/harddisk.png"));
      FOLDER = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/folder.png"));
      FOLDERS = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/folders.png"));
   }

   public DirectoryTreeCellRenderer () {

      directoryLabelMap = new HashMap<DirectoryNode, JLabel>();
   }

   public Component getTreeCellRendererComponent (JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

      JLabel directoryLabel;

      if ((directoryLabel = directoryLabelMap.get(value)) == null) {
         if (row == 0) {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getAbsolutePath(), DRIVE, SwingConstants.LEFT);
         }
         else if (leaf) {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getName(), FOLDER, SwingConstants.LEFT);
         }
         else {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getName(), FOLDERS, SwingConstants.LEFT);
         }

         directoryLabel.setBorder(INVISIBLE_BORDER);
         directoryLabel.setOpaque(true);
         directoryLabelMap.put((DirectoryNode)value, directoryLabel);
      }

      if (selected) {
         directoryLabel.setBackground(UIManager.getDefaults().getColor("textHighlight"));
         directoryLabel.setBorder(SELECTED_BORDER);
      }
      else {
         directoryLabel.setBackground(UIManager.getDefaults().getColor("text"));
         directoryLabel.setBorder(INVISIBLE_BORDER);
      }

      return directoryLabel;
   }

}
