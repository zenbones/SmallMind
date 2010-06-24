package org.smallmind.persistence.orm;

public interface ProxyTransaction {

   public abstract ProxySession getSession ();

   public abstract void commit ();

   public abstract void rollback ();

   public abstract boolean isCompleted ();

   public abstract boolean isRollbackOnly ();

   public abstract void setRollbackOnly ();
}
