package org.smallmind.nutsnbolts.util;

public interface Option<T> {

   public abstract boolean isNone ();

   public abstract T get ();
}
