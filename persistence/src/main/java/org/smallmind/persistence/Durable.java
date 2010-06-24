package org.smallmind.persistence;

import java.io.Serializable;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class Durable<I> implements Serializable {

   private I id;

   public synchronized I getId () {

      return id;
   }

   public synchronized void setId (I id) {

      this.id = id;
   }

   @Override
   public synchronized int hashCode () {

      int h = id.hashCode();

      h ^= (h >>> 20) ^ (h >>> 12);

      return h ^ (h >>> 7) ^ (h >>> 4);
   }

   @Override
   public synchronized boolean equals (Object obj) {

      if (obj instanceof Durable) {
         if ((((Durable)obj).getId() == null) || (id == null)) {
            return super.equals(obj);
         }
         else {
            return ((Durable)obj).getId().equals(id);
         }
      }

      return false;
   }
}
