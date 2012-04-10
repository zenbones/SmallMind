/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
