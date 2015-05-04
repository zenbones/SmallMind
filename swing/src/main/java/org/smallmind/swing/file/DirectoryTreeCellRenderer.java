/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.file;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.TreeCellRenderer;
import org.smallmind.swing.ColorUtilities;

public class DirectoryTreeCellRenderer implements TreeCellRenderer {

  private static final ImageIcon DRIVE = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/harddisk_16.png"));
  private static final ImageIcon FOLDER = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/folder_16.png"));
  private static final ImageIcon FOLDERS = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/folders_16.png"));

  private static Border SELECTED_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getDefaults().getColor("textHighlight").darker()), BorderFactory.createEmptyBorder(1, 1, 1, 1));
  private static Border INVISIBLE_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorUtilities.TEXT_COLOR), BorderFactory.createEmptyBorder(1, 1, 1, 1));

  public Component getTreeCellRendererComponent (JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JLabel directoryLabel;

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
