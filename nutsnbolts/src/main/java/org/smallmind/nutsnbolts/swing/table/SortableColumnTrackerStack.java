package org.smallmind.nutsnbolts.swing.table;

import java.util.Iterator;
import java.util.LinkedList;

public class SortableColumnTrackerStack<E extends Enum> implements Iterable<SortableColumnTracker<E>> {

   private LinkedList<SortableColumnTracker<E>> trackerList;

   public SortableColumnTrackerStack () {

      trackerList = new LinkedList<SortableColumnTracker<E>>();
   }

   public synchronized void addSortableColumnTracker (SortableColumnTracker<E> columnTracker) {

      trackerList.remove(columnTracker);
      trackerList.add(0, columnTracker);
   }

   public synchronized void removeSortableColumnTracker (E enumDataType) {

      Iterator<SortableColumnTracker<E>> columnTrackerIter;

      columnTrackerIter = iterator();
      while (columnTrackerIter.hasNext()) {
         if (columnTrackerIter.next().getEnumDataType().equals(enumDataType)) {
            columnTrackerIter.remove();
            break;
         }
      }
   }

   public Iterator<SortableColumnTracker<E>> iterator () {

      return trackerList.iterator();
   }

   public String toString () {

      StringBuilder displayBuilder;

      displayBuilder = new StringBuilder("TrackserStack[\n");
      for (SortableColumnTracker<E> tracker : trackerList) {
         displayBuilder.append(tracker.toString());
         displayBuilder.append('\n');
      }
      displayBuilder.append(']');

      return displayBuilder.toString();
   }

}
