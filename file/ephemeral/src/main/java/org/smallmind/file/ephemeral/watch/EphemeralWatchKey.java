package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.smallmind.file.ephemeral.EphemeralPath;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

public class EphemeralWatchKey implements WatchKey, HeapEventListener {

  private final EphemeralWatchService watchService;
  private final WatchEvent.Kind<?>[] events;
  private final WatchEvent.Modifier[] modifiers;
  private final EphemeralPath path;
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
  private boolean valid = true;

  public EphemeralWatchKey (EphemeralWatchService watchService, WatchEvent.Kind<?>[] events, WatchEvent.Modifier[] modifiers, EphemeralPath path) {

    this.watchService = watchService;
    this.events = events;
    this.modifiers = modifiers;
    this.path = path;
  }

  public EphemeralPath getPath () {

    return path;
  }

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

      return watchService.
    } else {

      return false;
    }
  }

  @Override
  public synchronized void cancel () {

    valid = false;
    watchService.unregister(this);
  }

  @Override
  public Watchable watchable () {

    return path;
  }
}
