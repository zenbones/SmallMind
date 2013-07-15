/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
