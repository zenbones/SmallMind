package org.smallmind.nutsnbolts.swing.catalog;

import org.smallmind.nutsnbolts.swing.event.MultiListSelectionListener;

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
