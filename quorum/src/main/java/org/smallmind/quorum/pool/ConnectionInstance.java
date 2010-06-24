package org.smallmind.quorum.pool;

public interface ConnectionInstance<C> {

   public abstract boolean validate ();

   public abstract C serve ()
      throws Exception;

   public abstract void close ()
      throws Exception;
}
