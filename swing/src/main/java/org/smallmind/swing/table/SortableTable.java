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
