package org.smallmind.nutsnbolts.swing.catalog;

import org.smallmind.nutsnbolts.swing.event.MultiListSelectionEvent;
import org.smallmind.nutsnbolts.swing.event.MultiListSelectionListener;

public class CatalogMultiListDataProvider<T extends Comparable<T>> implements MultiListDataProvider<T>, MultiListSelectionListener<T> {

   private T key;
   private Catalog catalog;

   public CatalogMultiListDataProvider (T key, Catalog catalog) {

      this.key = key;
      this.catalog = catalog;
   }

   public T getKey () {

      return key;
   }

   public int getElementCount () {

      return catalog.getModel().getSize();
   }

   public void valueChanged (MultiListSelectionEvent selectionEvent) {

      catalog.valueChanged(selectionEvent);
   }

}
