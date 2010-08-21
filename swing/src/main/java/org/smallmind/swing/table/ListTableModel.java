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
