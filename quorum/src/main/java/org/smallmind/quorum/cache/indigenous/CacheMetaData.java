package org.smallmind.quorum.cache.indigenous;

public interface CacheMetaData {

   /*
   * If willUpdate() returns true, update() is guaranteed to be called
   */
   public abstract boolean willUpdate ();

   public abstract void update ();
}
