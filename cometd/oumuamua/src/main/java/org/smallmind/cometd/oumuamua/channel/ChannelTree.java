package org.smallmind.cometd.oumuamua.channel;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelTree {

  private static final RemovalOperation REMOVAL_OPERATION = new RemovalOperation();
  private final ConcurrentHashMap<String, ChannelTree> childMap = new ConcurrentHashMap<>();
  private final String id;

  public ChannelTree (String id) {

    this.id = id;
  }

  public String getId () {

    return id;
  }

  public ChannelTree add (int index, String[] path) {

    if (index >= path.length) {

      return this;
    } else {

      ChannelTree child;

      if ((child = childMap.get(path[index])) == null) {
        childMap.putIfAbsent(path[index], child = new ChannelTree(path[index]));
      }

      return child.add(index + 1, path);
    }
  }

  public ChannelTree remove (int index, String[] path) {

    if (index == (path.length - 1)) {

      ChannelTree child;

      if ((child = childMap.remove(path[index])) != null) {
        child.walk(REMOVAL_OPERATION);
      }

      return child;
    } else {

      ChannelTree child;

      return ((child = childMap.get(path[index])) == null) ? null : child.remove(index + 1, path);
    }
  }

  public void walk (ChannelOperation operation) {

    operation.operate(this);

    for (ChannelTree channelTree : childMap.values()) {
      channelTree.walk(operation);
    }
  }

  public void operate (ChannelOperation operation) {

    operation.operate(this);
  }
}
