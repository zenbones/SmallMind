/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.table;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.smallmind.swing.label.PlainLabel;

public class SortableHeaderTableCellRenderer<E extends Enum> implements TableCellRenderer {

  private HashMap<Object, SortableHeaderPanel> renderMap;
  private boolean returnToNeutral;
  private boolean showOrder;

  public SortableHeaderTableCellRenderer (boolean returnToNeutral, boolean showOrder) {

    this.returnToNeutral = returnToNeutral;
    this.showOrder = showOrder;

    renderMap = new HashMap<Object, SortableHeaderPanel>();
  }

  public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

    SortableHeaderPanel headerPanel;

    if ((headerPanel = renderMap.get(value)) == null) {
      headerPanel = new SortableHeaderPanel<E>((SortableTable<E>)table, (E)value, new PlainLabel((String)table.getColumnModel().getColumn(column).getIdentifier(), JLabel.CENTER), returnToNeutral, showOrder);
      renderMap.put(value, headerPanel);
    }

    return headerPanel;
  }

}
