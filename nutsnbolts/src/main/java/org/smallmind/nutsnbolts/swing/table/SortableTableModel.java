package org.smallmind.nutsnbolts.swing.table;

import javax.swing.table.TableModel;

public interface SortableTableModel<E extends Enum> extends TableModel {

   public abstract void sortTableData (SortableColumnTrackerStack<E> trackerStack);

}
