package org.smallmind.swing.list;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

public class SelectableListSelectionModel extends DefaultListSelectionModel {

   private TableModel tableModel;
   private int columnIndex;

   public SelectableListSelectionModel (TableModel tableModel, int columnIndex) {

      super();

      this.tableModel = tableModel;
      this.columnIndex = columnIndex;
   }

   public void setSelectionInterval (int index0, int index1) {

      if (isRangeSelectable(index0, index1)) {
         super.setSelectionInterval(index0, index1);
      }
   }

   public void addSelectionInterval (int index0, int index1) {

      if (isRangeSelectable(index0, index1)) {
         super.addSelectionInterval(index0, index1);
      }
   }

   public void setAnchorSelectionIndex (int index) {

      if (isRangeSelectable(index, index)) {
         super.setAnchorSelectionIndex(index);
      }
   }

   public void setLeadSelectionIndex (int index) {

      if (getSelectionMode() != ListSelectionModel.SINGLE_SELECTION) {
         if (isRangeSelectable(index, index)) {
            super.setLeadSelectionIndex(index);
         }
      }
   }

   private boolean isRangeSelectable (int index0, int index1) {

      for (int count = index0; count <= index1; count++) {
         if (!((Selectable)tableModel.getValueAt(count, columnIndex)).isSelectable()) {
            return false;
         }
      }

      return true;
   }

}
