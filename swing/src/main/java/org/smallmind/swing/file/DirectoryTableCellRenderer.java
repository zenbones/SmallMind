/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.file;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import jakarta.swing.BorderFactory;
import jakarta.swing.ImageIcon;
import jakarta.swing.JLabel;
import jakarta.swing.JTable;
import jakarta.swing.table.TableCellRenderer;
import org.smallmind.swing.ColorUtility;

public class DirectoryTableCellRenderer implements TableCellRenderer {

  private static final ImageIcon TRIANGLE_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/grey_triangle_16.png"));
  private static final ImageIcon ROOT_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/harddisk_16.png"));

  @Override
  public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

    JLabel cellLabel;

    if (((File)value).getParentFile() == null) {
      cellLabel = new JLabel(((File)value).getAbsolutePath(), ROOT_ICON, JLabel.RIGHT);
    } else {
      cellLabel = new JLabel(((File)value).getName(), TRIANGLE_ICON, JLabel.RIGHT);
    }

    cellLabel.setOpaque(true);
    cellLabel.setVerticalAlignment(JLabel.BOTTOM);
    cellLabel.setVerticalTextPosition(JLabel.TOP);

    if (isSelected) {
      cellLabel.setBackground(ColorUtility.HIGHLIGHT_COLOR);
      cellLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), BorderFactory.createMatteBorder(1, 1, 3, 1, ColorUtility.HIGHLIGHT_COLOR)));
    } else {
      cellLabel.setBackground(table.getBackground());
      cellLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, table.getBackground()), BorderFactory.createMatteBorder(1, 1, 3, 1, table.getBackground())));
    }

    table.getColumnModel().getColumn(column).setPreferredWidth((int)cellLabel.getPreferredSize().getWidth());

    return cellLabel;
  }
}
