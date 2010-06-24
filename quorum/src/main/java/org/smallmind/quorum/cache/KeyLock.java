package org.smallmind.quorum.cache;

import java.util.UUID;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class KeyLock {

  private String uuidAsString;

  public KeyLock () {

    uuidAsString = UUID.randomUUID().toString();
  }

  public String getName () {

    return "Lock-" + uuidAsString;
  }

  public String getUUIDAsString () {

    return uuidAsString;
  }

  public int hashCode () {

    return uuidAsString.hashCode();
  }

  public boolean equals (Object obj) {

    return (obj instanceof KeyLock) && uuidAsString.equals(((KeyLock)obj).getUUIDAsString());
  }
}
