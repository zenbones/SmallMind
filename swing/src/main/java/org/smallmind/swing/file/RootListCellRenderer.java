/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class RootListCellRenderer implements ListCellRenderer {

  private static ImageIcon DRIVE;

  static {

    DRIVE = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/harddisk_16.png"));
  }

  public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

    JLabel rootLabel;

    rootLabel = new JLabel(((File)value).getAbsolutePath(), DRIVE, SwingConstants.LEFT);
    rootLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
    rootLabel.setOpaque(true);

    if (isSelected) {
      rootLabel.setBackground(UIManager.getDefaults().getColor("textHighlight"));
    }
    else {
      rootLabel.setBackground(UIManager.getDefaults().getColor("control"));
    }

    return rootLabel;
  }
}
