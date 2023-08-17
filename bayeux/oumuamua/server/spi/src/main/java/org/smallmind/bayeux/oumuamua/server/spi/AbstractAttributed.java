package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.HashMap;
import java.util.Set;
import org.smallmind.bayeux.oumuamua.server.api.Attributed;

public class AbstractAttributed implements Attributed {

  private final HashMap<String, Object> attributeMap = new HashMap<>();

  @Override
  public synchronized Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public synchronized Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public synchronized void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public synchronized Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }
}
