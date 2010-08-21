package org.smallmind.swing.catalog;

import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.DirectionalComparator;

public class MultiListSelectionStack<T extends Comparable<T>> {

   private LinkedList<MultiListSelection<T>> selectionList;
   private DirectionalComparator.Direction direction;

   public MultiListSelectionStack (DirectionalComparator.Direction direction) {

      this.direction = direction;

      selectionList = new LinkedList<MultiListSelection<T>>();
   }

   public synchronized void addMultiListSelection (MultiListSelection<T> selection) {

      Iterator<MultiListSelection<T>> selectionIter;
      int index = 0;

      if (selection != null) {
         selectionIter = selectionList.iterator();
         while (selectionIter.hasNext()) {
            if (selection.compareTo(selectionIter.next(), direction) < 0) {
               break;
            }
            else {
               index++;
            }
         }

         selectionList.add(index, selection);
      }
   }

   public synchronized MultiListSelection<T> getFirst () {

      if (selectionList.isEmpty()) {
         return null;
      }

      return selectionList.getFirst();
   }

   public synchronized MultiListSelection<T> getLast () {

      if (selectionList.isEmpty()) {
         return null;
      }

      return selectionList.getLast();
   }

}
