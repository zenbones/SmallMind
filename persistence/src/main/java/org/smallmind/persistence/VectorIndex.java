package org.smallmind.persistence;

public class VectorIndex<I extends Comparable<I>> {

   private Class<? extends Durable> indexClass;
   private I indexId;

   public VectorIndex (Durable<I> owner) {

      this(owner.getClass(), owner.getId());
   }

   public VectorIndex (Class<? extends Durable> indexClass, I indexId) {

      this.indexClass = indexClass;
      this.indexId = indexId;
   }

   public Class<? extends Durable> getIndexClass () {

      return indexClass;
   }

   public I getIndexId () {

      return indexId;
   }
}
