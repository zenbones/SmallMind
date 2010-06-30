package org.smallmind.persistence;

public class DurableKey<I extends Comparable<I>, D extends Durable<I>> {

   private Class<D> durableClass;
   private String key;

   public DurableKey (Class<D> durableClass, I id) {

      this.durableClass = durableClass;

      StringBuilder keyBuidler = new StringBuilder(durableClass.getSimpleName());

      keyBuidler.append('=');
      keyBuidler.append(id);

      key = keyBuidler.toString();
   }

   public Class<D> getDurableClass () {

      return durableClass;
   }

   public String getKey () {

      return key;
   }

   public String toString () {

      return key;
   }

   public int hashCode () {

      return key.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof DurableKey) && key.equals(((DurableKey)obj).getKey());
   }
}
