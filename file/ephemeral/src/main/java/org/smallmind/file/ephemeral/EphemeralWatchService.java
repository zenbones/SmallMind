package org.smallmind.file.ephemeral;

import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

public class EphemeralWatchService implements WatchService {

  @Override
  public void close () {

  }

  @Override
  public WatchKey poll () {

    return null;
  }

  @Override
  public WatchKey poll (long timeout, TimeUnit unit) {

    return null;
  }

  @Override
  public WatchKey take () {

    return null;
  }
}
