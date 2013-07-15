/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.wicket.component.google.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.smallmind.nutsnbolts.util.ArrayIterator;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class DataTable extends TableElement {

  private ColumnDescription[] columnDescriptions;
  private ArrayList<TableRow> rows;

  public DataTable (List<ColumnDescription> columnDescriptionList) {

    this(columnDescriptionList.toArray(new ColumnDescription[columnDescriptionList.size()]));
  }

  public DataTable (ColumnDescription[] columnDescriptions) {

    this.columnDescriptions = columnDescriptions;

    rows = new ArrayList<TableRow>();
  }

  public int getColumnCount () {

    return columnDescriptions.length;
  }

  public ColumnDescription getColumnDescription (int index) {

    return columnDescriptions[index];
  }

  public Iterable<ColumnDescription> getColumnDescriptions () {

    return new ArrayIterator<ColumnDescription>(columnDescriptions);
  }

  public synchronized TableRow createTableRow () {

    TableRow tableRow = new TableRow(this, new TableCell[columnDescriptions.length]);

    rows.add(tableRow);

    return tableRow;
  }

  public synchronized Iterable<TableRow> getRows () {

    return new IterableIterator<TableRow>(Collections.unmodifiableList(rows).iterator());
  }

  public synchronized TableRow getRow (int index) {

    return rows.get(index);
  }

  public synchronized TableCell getCell (int rowIndex, int cellIndex) {

    return rows.get(rowIndex).getCell(cellIndex);
  }

  public synchronized Value getValue (int rowIndex, int cellIndex) {

    return rows.get(rowIndex).getCell(cellIndex).getValue();
  }
}
