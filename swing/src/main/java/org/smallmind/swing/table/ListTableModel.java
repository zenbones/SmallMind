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
package org.smallmind.swing.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.smallmind.swing.list.Selectable;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class ListTableModel implements TableModel {

   private WeakEventListenerList<TableModelListener> listenerList;
   private List<Object> dataList;

   public ListTableModel () {

      this(new LinkedList<Object>());
   }

   public ListTableModel (List<?> dataList) {

      this.dataList = new ArrayList<Object>(dataList);

      listenerList = new WeakEventListenerList<TableModelListener>();
   }

   public synchronized void addTableModelListener (TableModelListener tableModelListener) {

      listenerList.addListener(tableModelListener);
   }

   public synchronized void removeTableModelListener (TableModelListener tableModelListener) {

      listenerList.removeListener(tableModelListener);
   }

   public synchronized int getRowCount () {

      return dataList.size();
   }

   public int getColumnCount () {

      return 1;
   }

   public String getColumnName (int columnIndex) {

      return null;
   }

   public Class getColumnClass (int columnIndex) {

      return Object.class;
   }

   public synchronized boolean isCellEditable (int rowIndex, int columnIndex) {

      return ((Selectable)dataList.get(rowIndex)).isSelectable();
   }

   public synchronized void clear () {

      if (dataList.size() > 0) {
         dataList.clear();
         fireTableChanged(new TableModelEvent(this));
      }
   }

   public synchronized Object getValueAt (int rowIndex, int columnIndex) {

      return dataList.get(rowIndex);
   }

   public synchronized void addValue (Object object) {

      dataList.add(object);
      fireTableChanged(new TableModelEvent(this, dataList.size() - 1, dataList.size() - 1, 0, TableModelEvent.INSERT));
   }

   public synchronized void setValueAt (Object object, int rowIndex, int columnIndex) {

      dataList.set(rowIndex, object);
      fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, 0, TableModelEvent.UPDATE));
   }

   public void fireTableChanged (TableModelEvent tableModelEvent) {

      Iterator<TableModelListener> listenerIter = listenerList.getListeners();

      while (listenerIter.hasNext()) {
         listenerIter.next().tableChanged(tableModelEvent);
      }
   }

}
