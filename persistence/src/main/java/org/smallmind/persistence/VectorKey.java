package org.smallmind.persistence;

import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class VectorKey<I, D extends Durable<I>> {

   private Class<D> elementClass;
   private String key;
   private String abstractKey;

   public VectorKey (Durable<I> owner, Class<D> elementClass) {

      this(owner.getClass(), owner.getId(), elementClass, null);
   }

   public VectorKey (Class<? extends Durable<I>> ownerClass, I ownerId, Class<D> elementClass) {

      this(ownerClass, ownerId, elementClass, null);
   }

   public VectorKey (Durable<I> owner, Class<D> elementClass, String classifier) {

      this(owner.getClass(), owner.getId(), elementClass, classifier);
   }

   public VectorKey (Class<? extends Durable> ownerClass, I ownerId, Class<D> elementClass, String classifier) {

      this.elementClass = elementClass;

      key = buildKey(ownerClass, ownerId, elementClass, classifier);
      abstractKey = buildAbstractKey(ownerClass, elementClass, classifier);
   }

   private String buildKey (Class<? extends Durable> ownerClass, I ownerId, Class<D> elementClass, String classifier) {
      StringBuilder keyBuilder;

      keyBuilder = new StringBuilder(elementClass.getSimpleName());
      keyBuilder.append('[');
      keyBuilder.append(ownerClass.getSimpleName());
      keyBuilder.append('=');
      keyBuilder.append(ownerId);
      keyBuilder.append(']');

      if (classifier != null) {
         keyBuilder.append(classifier);
      }

      return keyBuilder.toString();
   }

   public static String buildAbstractKey (Class<? extends Durable> ownerClass, Class<? extends Durable> elementClass, String classifier) {

      StringBuilder abstractKeyBuilder;

      abstractKeyBuilder = new StringBuilder(elementClass.getSimpleName());
      abstractKeyBuilder.append('[');
      abstractKeyBuilder.append(ownerClass.getSimpleName());
      abstractKeyBuilder.append(']');

      if (classifier != null) {
         abstractKeyBuilder.append(classifier);
      }

      return abstractKeyBuilder.toString();
   }

   public String getKey () {

      return key;
   }

   public String getAbstractKey () {

      return abstractKey;
   }

   public Class<D> getElementClass () {

      return elementClass;
   }
}