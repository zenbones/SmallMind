package org.smallmind.persistence.orm.hibernate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.hibernate.ScrollableResults;

public class ScrollIterator<T> implements Iterator<T>, Iterable<T> {

   private ScrollableResults scrollableResults;

   private Class<T> managedClass;

   private boolean more;

   public ScrollIterator (ScrollableResults scrollableResults, Class<T> managedClass) {

      this.scrollableResults = scrollableResults;
      this.managedClass = managedClass;

      more = scrollableResults.first();
   }

   public Iterator<T> iterator () {

      return this;
   }

   public boolean hasNext () {

      return more;
   }

   public T next () {

      if (!more) {
         throw new NoSuchElementException();
      }

      try {

         Object[] result;

         return managedClass.cast((managedClass.isArray() && ((result = scrollableResults.get()).length > 1)) ? result : scrollableResults.get(0));
      }
      finally {
         more = scrollableResults.next();
      }
   }

   public void remove () {

      throw new UnsupportedOperationException();
   }
}