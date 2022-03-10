package org.smallmind.memcached.cubby.locator;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;

public class DefaultKeyLocator implements KeyLocator {

  private final ServerPool serverPool;
  private String[] routingArray;

  public DefaultKeyLocator (ServerPool serverPool) {

    this.serverPool = serverPool;

    routingArray = generateRoutingArray(serverPool);
  }

  private String[] generateRoutingArray (ServerPool serverPool) {

    LinkedList<String> activeNameList = new LinkedList<>();

    for (MemcachedHost memcachedHost : serverPool.values()) {
      if (memcachedHost.isActive()) {
        activeNameList.add(memcachedHost.getName());
      }
    }

    if (activeNameList.isEmpty()) {

      return new String[0];
    } else {

      String[] activeNames;

      Collections.sort(activeNameList);
      activeNames = activeNameList.toArray(new String[0]);

      return activeNames;
    }
  }

  @Override
  public MemcachedHost find (String key)
    throws IOException {

    if (routingArray.length == 0) {
      throw new NoAvailableHostException();
    } else {

      return serverPool.get(routingArray[key.hashCode() % routingArray.length]);
    }
  }
}
