package org.smallmind.file.ephemeral.heap;

import java.util.EventObject;
import org.smallmind.file.ephemeral.EphemeralPath;

public class HeapEvent extends EventObject {

  private final HeapEventType type;
  private final EphemeralPath path;

  public HeapEvent (Object source, EphemeralPath path, HeapEventType type) {

    super(source);

    this.path = path;
    this.type = type;
  }

  public HeapEventType getType () {

    return type;
  }

  public EphemeralPath getPath () {

    return path;
  }
}
