package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;

public class EphemeralWatchEvent<T> implements WatchEvent<T> {

  private final Kind<T> kind;
  private final T context;
  private final int count;

  public EphemeralWatchEvent (Kind<T> kind, int count, T context) {

    this.kind = kind;
    this.count = count;
    this.context = context;
  }

  @Override
  public Kind<T> kind () {

    return kind;
  }

  @Override
  public int count () {

    return count;
  }

  @Override
  public T context () {

    return context;
  }
}
