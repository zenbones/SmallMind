package org.smallmind.file.ephemeral.heap;

import java.nio.file.Path;
import java.util.EventObject;

public class HeapEvent extends EventObject {

  private final HeapEventType type;
  private final Path path;

  public HeapEvent (Object source, Path path, HeapEventType type) {

    super(source);

    this.path = path;
    this.type = type;
  }

  public HeapEventType getType () {

    return type;
  }

  public Path getPath () {

    return path;
  }
}
