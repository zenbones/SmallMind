package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;

public class EphemeralWatchEvent<T> implements WatchEvent<T> {

  @Override
  public Kind<T> kind () {

    return null;
  }

  @Override
  public int count () {

    return 0;
  }

  @Override
  public T context () {

    return null;
  }
}
