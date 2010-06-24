package org.smallmind.nutsnbolts.swing.catalog;

import org.smallmind.nutsnbolts.swing.event.CatalogDataListener;

public interface CatalogModel<D> {

   public abstract void addCatalogDataListener (CatalogDataListener catalogDataListener);

   public abstract void removeCatalogDataListener (CatalogDataListener catalogDataListener);

   public abstract D getElementAt (int index);

   public int indexOf (D element);

   public abstract int getSize ();

}
