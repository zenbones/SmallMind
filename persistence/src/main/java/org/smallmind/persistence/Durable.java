package org.smallmind.persistence;

import java.io.Serializable;
import org.terracotta.modules.annotations.AutolockRead;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class Durable<I extends Comparable<I>> implements Serializable, Comparable<Durable<I>> {

   private I id;

   public Durable () {
   }

   public Durable (Durable<I> durable) {

      id = durable.getId();
   }

   @AutolockRead
   public synchronized I getId () {

      return id;
   }

   @AutolockWrite
   public synchronized void setId (I id) {

      this.id = id;
   }

   public int compareTo (Durable<I> durable) {

      if (getId() == null) {
         if (durable.getId() == null) {

            return 0;
         }
         else {

            return -1;
         }
      }

      if (durable.getId() == null) {

         return 1;
      }

      return durable.getId().compareTo(getId());
   }

   @Override
   @AutolockRead
   public synchronized int hashCode () {

      if (id == null) {

         return super.hashCode();
      }

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
