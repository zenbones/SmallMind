package org.smallmind.file.ephemeral.heap;

import java.util.EventListener;

public interface HeapEventListener extends EventListener {

  void handle (HeapEvent heapEvent);
}
