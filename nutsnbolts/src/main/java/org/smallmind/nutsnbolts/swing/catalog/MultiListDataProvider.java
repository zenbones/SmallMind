package org.smallmind.nutsnbolts.swing.catalog;

public interface MultiListDataProvider<T extends Comparable<T>> {

   public abstract T getKey ();

   public abstract int getElementCount ();

}
