package org.smallmind.persistence.orm.aop;

public class RollbackAwareBoundarySet<T> extends BoundarySet<T> {

   private boolean rollbackOnly;

   public RollbackAwareBoundarySet (String dataSources[], boolean implicit, boolean rollbackOnly) {

      super(dataSources, implicit);

      this.rollbackOnly = rollbackOnly;
   }

   public boolean isRollbackOnly () {

      return rollbackOnly;
   }
}