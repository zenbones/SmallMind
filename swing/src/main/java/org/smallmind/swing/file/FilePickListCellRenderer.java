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
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.smallmind.swing.ColorUtility;

public class FilePickListCellRenderer implements ListCellRenderer {

  private static final ImageIcon DOCUMENT_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/document_16.png"));
  private static final ImageIcon FOLDER_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/folder_closed_16.png"));

  private final AtomicInteger rowHeight = new AtomicInteger(0);

  @Override
  public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

    JLabel cellLabel;

    cellLabel = new JLabel((((File)value).getName().length() > 20) ? ((File)value).getName().substring(0, 20) + "..." : ((File)value).getName(), ((File)value).isDirectory() ? FOLDER_ICON : DOCUMENT_ICON, JLabel.LEFT);
    cellLabel.setOpaque(true);
    cellLabel.setBackground(isSelected ? ColorUtility.HIGHLIGHT_COLOR : list.getBackground());
    cellLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, isSelected ? Color.LIGHT_GRAY : list.getBackground()), BorderFactory.createMatteBorder(1, 1, 1, 1, isSelected ? ColorUtility.HIGHLIGHT_COLOR : list.getBackground())));
    cellLabel.setToolTipText(((File)value).getName());
    rowHeight.set((int)cellLabel.getPreferredSize().getHeight());

    return cellLabel;
  }

  public int getRowHeight () {

    return rowHeight.get();
  }
}
