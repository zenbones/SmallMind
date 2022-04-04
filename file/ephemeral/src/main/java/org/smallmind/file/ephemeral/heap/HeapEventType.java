package org.smallmind.file.ephemeral.heap;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public enum HeapEventType {

  CREATE(StandardWatchEventKinds.ENTRY_CREATE), DELETE(StandardWatchEventKinds.ENTRY_DELETE), MODIFY(StandardWatchEventKinds.ENTRY_MODIFY);

  private final WatchEvent.Kind<?> kind;

  HeapEventType (WatchEvent.Kind<?> kind) {

    this.kind = kind;
  }

  public WatchEvent.Kind<?> getKind () {

    return kind;
  }
}
