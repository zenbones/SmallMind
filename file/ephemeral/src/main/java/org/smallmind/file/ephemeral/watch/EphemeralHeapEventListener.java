package org.smallmind.file.ephemeral.watch;

import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

public class EphemeralHeapEventListener implements HeapEventListener {

  private final EphemeralWatchService watchService;

  public EphemeralHeapEventListener (EphemeralWatchService watchService) {

    this.watchService = watchService;
  }

  @Override
  public void handle (HeapEvent heapEvent) {

    watchService.fire(heapEvent.getPath(), heapEvent.getType().getKind());
  }
}
