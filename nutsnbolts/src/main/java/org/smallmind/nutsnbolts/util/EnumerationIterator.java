package org.smallmind.nutsnbolts.util;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterator<T> implements Iterator<T>, Iterable<T> {

   private Enumeration<T> internalEnumeration;

   public EnumerationIterator(Enumeration<T> internalEnumeration) {

      this.internalEnumeration = internalEnumeration;
   }

   public boolean hasNext() {

      return internalEnumeration.hasMoreElements();
   }

   public T next() {

      return internalEnumeration.nextElement();
   }

   public void remove() {

      throw new UnsupportedOperationException();
   }

   public Iterator<T> iterator() {

      return this;
   }
}
