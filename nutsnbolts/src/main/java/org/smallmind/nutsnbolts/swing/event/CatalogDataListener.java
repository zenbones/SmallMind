package org.smallmind.nutsnbolts.swing.event;

import java.util.EventListener;

public interface CatalogDataListener extends EventListener {

   public abstract void itemAdded (CatalogDataEvent catalogDataEvent);

   public abstract void intervalRemoved (CatalogDataEvent catalogDataEvent);

   public abstract void intervalChanged (CatalogDataEvent catalogDataEvent);

}
