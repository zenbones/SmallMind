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
package org.smallmind.swing.catalog;

import org.smallmind.swing.event.MultiListSelectionListener;

public interface MultiListSelectionModel<T extends Comparable<T>> {

   public static enum SelctionMode {

      SINGLE_SELECTION, SINGLE_INTERVAL_SELECTION, MULTIPLE_INTERVAL_SELECTION
   }

   public abstract void clearMultiListDataProviders ();

   public abstract void addMultiListDataProvider (MultiListDataProvider<T> dataProvider);

   public abstract void removeMultiListDataProvider (MultiListDataProvider<T> dataProvider);

   public abstract void clearMultiListSelectionListeners ();

   public abstract void addMultiListSelectionListener (MultiListSelectionListener<T> selectionListener);

   public abstract void removeMultiListSelectionListener (MultiListSelectionListener<T> selectionListener);

   public abstract void setSelectionMode (SelctionMode selectionSelctionMode);

   public abstract SelctionMode getSelectionMode ();

   public abstract void setValueIsAdjusting (boolean valueIsAdjusting);

   public abstract boolean getValueIsAdjusting ();

   public abstract boolean isSelectionEmpty ();

   public abstract void clearSelection ();

   public abstract MultiListSelection<T> getMinSelection ();

   public abstract MultiListSelection<T> getMaxSelection ();

   public abstract boolean isSelected (MultiListSelection<T> selection);

   public abstract void setAnchorSelection (MultiListSelection<T> selection);

   public abstract MultiListSelection<T> getAnchorSelection ();

   public abstract void setLeadSelection (MultiListSelection<T> selection);

   public abstract MultiListSelection<T> getLeadSelection ();

   public abstract int getSelectedIndex (T key);

   public abstract void addSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1);

   public abstract void setSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1);

   public abstract void removeSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1);

   public abstract void insertIndexInterval (MultiListSelection<T> selection, int length, boolean before);

   public abstract void removeIndexInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1);

}
