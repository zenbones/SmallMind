package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.smallmind.file.ephemeral.EphemeralPath;

public class EphemeralWatchKey implements WatchKey {

  private final EphemeralWatchService watchService;
  private final WatchEvent.Kind<?>[] events;
  private final WatchEvent.Modifier[] modifiers;
  private final EphemeralPath path;
  private final LinkedBlockingQueue<WatchEvent<?>> eventQueue = new LinkedBlockingQueue<>();
  private boolean valid = true;
  private boolean signalled = false;

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

    return valid && (!watchService.isClosed());
  }

  public synchronized boolean fire (WatchEvent.Kind<?> firedEvent) {

    if (valid) {
      for (WatchEvent.Kind<?> event : events) {
        if (event.getClass().equals(firedEvent.getClass()) && event.name().equals(firedEvent.name())) {
          eventQueue.add(new EphemeralWatchEvent<>(firedEvent, 1, null));

          if (!signalled) {
            signalled = true;

            return true;
          } else {

            return false;
          }
        }
      }
    }

    return false;
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

      if (!eventQueue.isEmpty()) {
        watchService.requeue(this);
      } else {
        signalled = false;
      }

      return true;
    } else {

      return false;
    }
  }

  @Override
  public synchronized void cancel () {

    cancel(true);
  }

  public synchronized void cancel (boolean deregister) {

    valid = false;

    if (deregister) {
      watchService.unregister(this);
    }
  }

  @Override
  public Watchable watchable () {

    return path;
  }
}
