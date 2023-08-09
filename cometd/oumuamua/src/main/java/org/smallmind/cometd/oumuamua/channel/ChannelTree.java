/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.cometd.oumuamua.channel;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.cometd.bayeux.ChannelId;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public class ChannelTree {

  private final ReentrantLock treeExpansionLock = new ReentrantLock();
  private final ReentrantReadWriteLock channelChangeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<String, ChannelTree> childMap = new ConcurrentHashMap<>();
  private final ChannelTree parent;
  private OumuamuaServerChannel serverChannel;

  public ChannelTree () {

    this(null, null);
  }

  public ChannelTree (ChannelTree parent, OumuamuaServerChannel serverChannel) {

    this.parent = parent;
    this.serverChannel = serverChannel;
  }

  public OumuamuaServerChannel getServerChannel () {

    channelChangeLock.readLock().lock();

    try {
      return serverChannel;
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  private void send (OumuamuaTransport transport, OumuamuaPacket packet, HashSet<String> sessionIdSet) {

    channelChangeLock.readLock().lock();

    try {
      if (serverChannel != null) {
        serverChannel.send(transport, packet, sessionIdSet);
      }
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  private ChannelTree enforceServerChannel (OumuamuaServer oumuamuaServer, ChannelId channelId) {

    channelChangeLock.writeLock().lock();

    try {
      if (serverChannel == null) {
        serverChannel = new OumuamuaServerChannel(oumuamuaServer, channelId);
      }

      return this;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  // Should not actually be used publicly, but called only under the server's change lock, as in ExpirationOperator.operate()
  public ChannelTree removeServerChannel () {

    channelChangeLock.writeLock().lock();

    try {
      serverChannel = null;

      return this;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  private void clipChild (String key) {

    childMap.remove(key);
  }

  public ChannelTree find (int index, ChannelId channelId) {

    ChannelTree child;

    if ((child = childMap.get(channelId.getSegment(index))) == null) {

      return null;
    } else {

      return (index == (channelId.depth() - 1)) ? child : child.find(index + 1, channelId);
    }
  }

  public ChannelTree createIfAbsent (OumuamuaServer oumuamuaServer, int index, ChannelId channelId) {

    ChannelTree child;

    if ((child = childMap.get(channelId.getSegment(index))) == null) {

      treeExpansionLock.lock();

      try {
        if ((child = childMap.get(channelId.getSegment(index))) == null) {

          ChannelId branchChannelId = (index == (channelId.depth() - 1)) ? channelId : ChannelIdUtility.from(index + 1, channelId);

          childMap.put(channelId.getSegment(index), child = new ChannelTree(this, (index == (channelId.depth() - 1)) ? new OumuamuaServerChannel(oumuamuaServer, branchChannelId) : null));
        }
      } finally {
        treeExpansionLock.unlock();
      }
    }

    return (index == (channelId.depth() - 1)) ? child.enforceServerChannel(oumuamuaServer, channelId) : child.createIfAbsent(oumuamuaServer, index + 1, channelId);
  }

  public ChannelTree removeIfPresent (int index, ChannelId channelId) {

    ChannelTree child;

    if ((child = childMap.get(channelId.getSegment(index))) == null) {

      return null;
    } else {

      return (index == (channelId.depth() - 1)) ? child.removeServerChannel() : child.removeIfPresent(index + 1, channelId);
    }
  }

  public void trim (String key) {

    // Using getServerChannel() to enforce synchronization boundary
    if ((parent != null) && (getServerChannel() == null) && childMap.isEmpty()) {
      parent.clipChild(key);
    } else {
      for (Map.Entry<String, ChannelTree> childEntry : childMap.entrySet()) {
        childEntry.getValue().trim(childEntry.getKey());
      }
    }
  }

  public void publish (OumuamuaTransport transport, ChannelIterator channelIterator, OumuamuaPacket packet, HashSet<String> sessionIdSet) {

    if (channelIterator.hasNext()) {

      ChannelTree deepWildBranch;
      ChannelTree nextBranch;

      if ((deepWildBranch = childMap.get(ChannelId.DEEPWILD)) != null) {

        deepWildBranch.send(transport, packet, sessionIdSet);
      }
      if ((nextBranch = childMap.get(channelIterator.next())) != null) {
        nextBranch.publish(transport, channelIterator, packet, sessionIdSet);
      }
    } else {
      if (parent != null) {

        ChannelTree wildBranch;

        if ((wildBranch = parent.childMap.get(ChannelId.WILD)) != null) {
          wildBranch.send(transport, packet, sessionIdSet);
        }
      }

      send(transport, packet, sessionIdSet);
    }
  }

  public void walk (ChannelOperation operation) {

    operation.operate(this);

    for (ChannelTree child : childMap.values()) {
      child.walk(operation);
    }
  }
}
