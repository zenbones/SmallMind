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
package org.smallmind.swing.catalog;

import org.smallmind.nutsnbolts.util.DirectionalComparator;

public class MultiListSelection<T extends Comparable<T>> {

  private T comparable;
  private int index;

  public MultiListSelection (T comparable, int index) {

    this.comparable = comparable;
    this.index = index;
  }

  public T getComparable () {

    return comparable;
  }

  public int getIndex () {

    return index;
  }

  public int compareTo (MultiListSelection<T> selection, DirectionalComparator.Direction direction) {

    if (getComparable().compareTo(selection.getComparable()) < 0) {
      if (direction.equals(DirectionalComparator.Direction.ASCENDING)) {
        return 1;
      }
      else {
        return -1;
      }
    }
    else if (getComparable().compareTo(selection.getComparable()) > 0) {
      if (direction.equals(DirectionalComparator.Direction.ASCENDING)) {
        return -1;
      }
      else {
        return 1;
      }
    }
    else {
      return getIndex() - selection.getIndex();
    }
  }

}
