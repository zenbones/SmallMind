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
