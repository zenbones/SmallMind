package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;

public class EphemeralWatchable implements Watchable {

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {

    return null;
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>... events) {

    return null;
  }
}
