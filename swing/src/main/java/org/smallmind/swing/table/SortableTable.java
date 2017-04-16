/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class SortableTable<E extends Enum> extends JTable implements MouseListener {

  private SortableColumnTrackerStack<E> trackerStack;

  public SortableTable (SortableTableModel<E> sortableTableModel) {

    super(sortableTableModel);

    trackerStack = new SortableColumnTrackerStack<E>();
  }

  public synchronized SortableTableModel<E> getModel () {

    return (SortableTableModel<E>)super.getModel();
  }

  public synchronized void setModel (SortableTableModel<E> sortableTableModel) {

    super.setModel(sortableTableModel);
  }

  public synchronized void setTableHeader (JTableHeader tableHeader) {

    if (getTableHeader() != null) {
      getTableHeader().removeMouseListener(this);
    }

    super.setTableHeader(tableHeader);

    if (tableHeader != null) {
      updateHeader();
      tableHeader.addMouseListener(this);
    }
  }

  public synchronized void removeSortableColumnTracker (E enumDataType) {

    trackerStack.removeSortableColumnTracker(enumDataType);
  }

  public synchronized int getSortOrder (E enumDataType) {

    int pos = 0;

    for (SortableColumnTracker columnTracker : trackerStack) {
      if (columnTracker.getEnumDataType().equals(enumDataType)) {
        return pos;
      }

      pos++;
    }

    return -1;
  }

  public synchronized SortableDirection getSortDirection (E enumDataType) {

    for (SortableColumnTracker columnTracker : trackerStack) {
      if (columnTracker.getEnumDataType().equals(enumDataType)) {
        return columnTracker.getDirection();
      }
    }

    return SortableDirection.NONE;
  }

  public synchronized void setSortDirection (E enumDataType, SortableDirection direction) {

    if (direction.equals(SortableDirection.NONE)) {
      trackerStack.removeSortableColumnTracker(enumDataType);
    }
    else {
      trackerStack.addSortableColumnTracker(new SortableColumnTracker<E>(enumDataType, direction));
    }

    updateHeader();
  }

  public synchronized void updateHeader () {

    TableColumn browseColumn;
    Component renderComponent;

    for (int count = 0; count < getColumnModel().getColumnCount(); count++) {
      browseColumn = getColumnModel().getColumn(count);
      if (browseColumn.getHeaderRenderer() != null) {
        renderComponent = browseColumn.getHeaderRenderer().getTableCellRendererComponent(this, browseColumn.getHeaderValue(), false, false, 0, count);
        ((SortableHeaderPanel)renderComponent).displaySortingState();
      }
    }
  }

  public synchronized void sortTableData () {

    getModel().sortTableData(trackerStack);
  }

  public void mouseEntered (MouseEvent mouseEvent) {

  }

  public void mouseExited (MouseEvent mouseEvent) {

  }

  public void mouseClicked (MouseEvent mouseEvent) {

  }

  public void mouseReleased (MouseEvent mouseEvent) {

  }

  public synchronized void mousePressed (MouseEvent mouseEvent) {

    TableColumn browseColumn;
    Component renderComponent;
    int columnIndex;
    int leftClipPos = 0;
    int hitPoint;

    columnIndex = columnAtPoint(mouseEvent.getPoint());
    for (int count = 0; count < columnIndex; count++) {
      leftClipPos += getColumnModel().getColumn(count).getWidth();
    }

    browseColumn = getColumnModel().getColumn(columnIndex);
    renderComponent = browseColumn.getHeaderRenderer().getTableCellRendererComponent(this, browseColumn.getHeaderValue(), false, false, 0, columnIndex);
    hitPoint = leftClipPos + browseColumn.getWidth() - mouseEvent.getX();
    if ((hitPoint <= ((SortableHeaderPanel)renderComponent).getClickWidth()) && (hitPoint > 5)) {
      ((SortableHeaderPanel)renderComponent).incSortingState();
      getTableHeader().repaint();
    }
  }

}
