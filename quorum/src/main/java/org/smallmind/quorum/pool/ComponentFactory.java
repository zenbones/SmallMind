package org.smallmind.quorum.pool;

public interface ComponentFactory<T> {

   public abstract T createComponent ()
      throws Exception;
}
