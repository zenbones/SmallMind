package org.smallmind.cometd.oumuamua.channel;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelBranch {

  private static final RemovalOperation REMOVAL_OPERATION = new RemovalOperation();
  private final ConcurrentHashMap<String, ChannelBranch> childMap = new ConcurrentHashMap<>();
  private final String id;

  public ChannelBranch (String id) {

    this.id = id;
  }

  public String getId () {

    return id;
  }

  public ChannelBranch add (int index, String[] path) {

    if (index >= path.length) {

      return this;
    } else {

      ChannelBranch child;

      if ((child = childMap.get(path[index])) == null) {
        childMap.putIfAbsent(path[index], child = new ChannelBranch(path[index]));
      }

      return child.add(index + 1, path);
    }
  }

  public ChannelBranch remove (int index, String[] path) {

    if (index == (path.length - 1)) {

      ChannelBranch child;

      if ((child = childMap.remove(path[index])) != null) {
        child.walk(REMOVAL_OPERATION);
      }

      return child;
    } else {

      ChannelBranch child;

      return ((child = childMap.get(path[index])) == null) ? null : child.remove(index + 1, path);
    }
  }

  public void walk (ChannelOperation operation) {

    operation.operate(this);

    for (ChannelBranch channelBranch : childMap.values()) {
      channelBranch.walk(operation);
    }
  }

  public void operate (ChannelOperation operation) {

    operation.operate(this);
  }
}
