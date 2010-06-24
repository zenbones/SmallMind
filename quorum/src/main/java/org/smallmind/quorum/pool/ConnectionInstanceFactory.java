package org.smallmind.quorum.pool;

public interface ConnectionInstanceFactory<C> {

   public abstract Object rawInstance ()
      throws Exception;

   public abstract ConnectionInstance<C> createInstance (ConnectionPool<C> connectionPool)
      throws Exception;
}
