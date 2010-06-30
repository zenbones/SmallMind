package org.smallmind.persistence;

import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class VectorKey<D extends Durable> {

   private Class<D> elementClass;
   private String key;

   public VectorKey (VectorIndex[] vectorIndices, Class<D> elementClass) {

      this(vectorIndices, elementClass, null);
   }

   public VectorKey (VectorIndex[] vectorIndices, Class<D> elementClass, String classification) {

      this.elementClass = elementClass;

      key = buildKey(vectorIndices, classification);
   }

   public String getKey () {

      return key;
   }

   public Class<D> getElementClass () {

      return elementClass;
   }

   private String buildKey (VectorIndex[] vectorIndices, String classification) {

      StringBuilder keyBuilder;
      boolean indexed = false;

      keyBuilder = new StringBuilder(elementClass.getSimpleName());

      keyBuilder.append('[');
      for (VectorIndex index : vectorIndices) {
         if (indexed) {
            keyBuilder.append(',');
         }

         keyBuilder.append(index.getIndexClass().getSimpleName());
         keyBuilder.append('=');
         keyBuilder.append(index.getIndexId());

         indexed = true;
      }
      keyBuilder.append(']');

      if (classification != null) {
         keyBuilder.append(classification);
      }

      return keyBuilder.toString();
   }

   public String toString () {

      return key;
   }

   public int hashCode () {

      return key.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof VectorKey) && key.equals(((VectorKey)obj).getKey());
   }
}