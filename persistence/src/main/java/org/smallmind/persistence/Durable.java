package org.smallmind.persistence;

import org.terracotta.modules.annotations.AutolockRead;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.InstrumentedClass;

import java.io.Serializable;
import java.lang.reflect.Field;

@InstrumentedClass
public abstract class Durable<I extends Comparable<I>> implements Serializable, Comparable<Durable<I>> {

   private I id;

   public Durable() {
   }

   public Durable(Durable<I> durable) {

      id = durable.getId();
   }

   @AutolockRead
   public synchronized I getId() {

      return id;
   }

   @AutolockWrite
   public synchronized void setId(I id) {

      this.id = id;
   }

   public int compareTo(Durable<I> durable) {

      if (getId() == null) {
         if (durable.getId() == null) {

            return 0;
         } else {

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
   public synchronized int hashCode() {

      if (id == null) {

         return super.hashCode();
      }

      int h = id.hashCode();

      h ^= (h >>> 20) ^ (h >>> 12);

      return h ^ (h >>> 7) ^ (h >>> 4);
   }

   @Override
   public synchronized boolean equals(Object obj) {

      if (obj instanceof Durable) {
         if ((((Durable) obj).getId() == null) || (id == null)) {
            return super.equals(obj);
         } else {
            return ((Durable) obj).getId().equals(id);
         }
      }

      return false;
   }

   public boolean mirrors(Durable durable) {

      return mirrors(durable, "id");
   }

   private boolean mirrors(Durable durable, String... exclusions) {

      if (this.getClass().isAssignableFrom(durable.getClass())) {

         boolean excluded;

         try {
            for (Field field : DurableFields.getFields(this.getClass())) {

               excluded = false;

               if ((exclusions != null) && (exclusions.length > 0)) {
                  for (String exclusion : exclusions) {
                     if (exclusion.equals(field.getName())) {
                        excluded = true;
                        break;
                     }
                  }
               }

               if (!excluded) {

                  Object myValue = field.get(this);
                  Object theirValue = field.get(durable);

                  if ((myValue == null)) {
                     if (theirValue != null) {

                        return false;
                     }
                  } else if (!myValue.equals(theirValue)) {

                     return false;
                  }
               }
            }
         }
         catch (IllegalAccessException illegalAccessException) {
            throw new RuntimeException(illegalAccessException);
         }


         return true;
      }

      return false;
   }

   @Override
   public String toString() {

      StringBuilder displayBuilder = new StringBuilder();
      boolean first = false;

      displayBuilder.append(this.getClass().getSimpleName()).append('[');

      try {
         for (Field field : DurableFields.getFields(this.getClass())) {
            if (first) {
               displayBuilder.append(',');
            }

            displayBuilder.append(field.getName()).append('=').append(field.get(this));
            first = true;
         }
      }
      catch (IllegalAccessException illegalAccessException) {
         throw new RuntimeException(illegalAccessException);
      }

      displayBuilder.append(']');


      return displayBuilder.toString();
   }
}
