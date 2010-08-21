package org.smallmind.swing.catalog;

import org.smallmind.swing.event.CatalogDataListener;

public interface CatalogModel<D> {

   public abstract void addCatalogDataListener (CatalogDataListener catalogDataListener);

   public abstract void removeCatalogDataListener (CatalogDataListener catalogDataListener);

   public abstract D getElementAt (int index);

   public int indexOf (D element);

   public abstract int getSize ();

}
