package org.smallmind.quorum.cache;

public interface LockedCallback<K, R> {

  public abstract K getKey ();

  public abstract R execute ();
}
