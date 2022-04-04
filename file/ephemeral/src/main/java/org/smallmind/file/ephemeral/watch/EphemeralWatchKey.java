package org.smallmind.file.ephemeral.watch;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class EphemeralWatchKey implements WatchKey {

  private final EphemeralWatchService watchService;
  private final WatchEvent.Kind<?>[] events;
  private final WatchEvent.Modifier[] modifiers;
  private final Path path;
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
  private boolean valid = true;

  @Override
  public synchronized boolean isValid () {

    return valid && (!watchService.isCosed());
  }

  @Override
  public List<WatchEvent<?>> pollEvents () {

    LinkedList<WatchEvent<?>> eventList = new LinkedList<>();
    WatchEvent<?> event;

    while ((event = eventQueue.poll()) != null) {
      eventList.add(event);
    }

    return eventList;
  }

  @Override
  public synchronized boolean reset () {

    if (isValid()) {
      watchService.
    }

    return false;
  }

  @Override
  public synchronized void cancel () {

    valid = false;
  }

  @Override
  public Watchable watchable () {

    return null;
  }
}
