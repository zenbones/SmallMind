/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.swing.table;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.swing.label.PlainLabel;

public class SortableHeaderPanel<E extends Enum> extends JPanel {

  private static final ImageIcon NONE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/unknown.png"));
  private static final ImageIcon DESCENDING = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/business and data/16x16/plain/sort_descending.png"));
  private static final ImageIcon ASCENDING = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/business and data/16x16/plain/sort_ascending.png"));

  private final SortableTable<E> sortableTable;
  private final E enumDataType;
  private final PlainLabel sortingLabel;
  private final boolean returnToNeutral;
  private final boolean showOrder;
  private int clickWidth = 0;

  public SortableHeaderPanel (SortableTable<E> sortableTable, E enumDataType, JComponent headerComponent, boolean returnToNeutral, boolean showOrder) {

    super(new GridBagLayout());

    GridBagConstraints constraint;

    this.sortableTable = sortableTable;
    this.enumDataType = enumDataType;
    this.returnToNeutral = returnToNeutral;
    this.showOrder = showOrder;

    setBackground(SystemColor.control);

    headerComponent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlDkShadow), BorderFactory.createMatteBorder(0, 1, 0, 0, SystemColor.controlLtHighlight)), BorderFactory.createEmptyBorder(2, 2, 2, 2)));

    sortingLabel = new PlainLabel();
    sortingLabel.setFontSize(12.0F);
    sortingLabel.setHorizontalAlignment(JLabel.RIGHT);
    sortingLabel.setVerticalAlignment(JLabel.BOTTOM);
    sortingLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.controlDkShadow), BorderFactory.createEmptyBorder(2, 2, 2, 5)));

    constraint = new GridBagConstraints();

    constraint.anchor = GridBagConstraints.WEST;
    constraint.fill = GridBagConstraints.BOTH;
    constraint.gridx = 0;
    constraint.gridy = 0;
    constraint.weightx = 1;
    constraint.weighty = 1;
    add(headerComponent, constraint);

    constraint.anchor = GridBagConstraints.EAST;
    constraint.fill = GridBagConstraints.VERTICAL;
    constraint.gridx = 1;
    constraint.gridy = 0;
    constraint.weightx = 0;
    constraint.weighty = 1;
    add(sortingLabel, constraint);

    setToolTipText(headerComponent.getToolTipText());

    displaySortingState();
  }

  public synchronized int getClickWidth () {

    return clickWidth;
  }

  public synchronized void displaySortingState () {

    switch (sortableTable.getSortDirection(enumDataType)) {
      case NONE:
        sortingLabel.setIcon(NONE);
        sortingLabel.setText(null);
        break;
      case DESCENDING:
        sortingLabel.setIcon(DESCENDING);
        if (showOrder) {
          sortingLabel.setText("[" + (sortableTable.getSortOrder(enumDataType) + 1) + "]");
        }
        break;
      case ASCENDING:
        sortingLabel.setIcon(ASCENDING);
        if (showOrder) {
          sortingLabel.setText("[" + (sortableTable.getSortOrder(enumDataType) + 1) + "]");
        }
        break;
      default:
        throw new UnknownSwitchCaseException(sortableTable.getSortDirection(enumDataType).name());
    }

    clickWidth = (int)sortingLabel.getPreferredSize().getWidth();
  }

  public synchronized void incSortingState () {

    switch (sortableTable.getSortDirection(enumDataType)) {
      case NONE:
        sortableTable.setSortDirection(enumDataType, SortableDirection.DESCENDING);
        break;
      case DESCENDING:
        sortableTable.setSortDirection(enumDataType, SortableDirection.ASCENDING);
        break;
      case ASCENDING:
        sortableTable.setSortDirection(enumDataType, (returnToNeutral) ? SortableDirection.NONE : SortableDirection.DESCENDING);
        break;
      default:
        throw new UnknownSwitchCaseException(sortableTable.getSortDirection(enumDataType).name());
    }

    sortableTable.sortTableData();
  }
}
