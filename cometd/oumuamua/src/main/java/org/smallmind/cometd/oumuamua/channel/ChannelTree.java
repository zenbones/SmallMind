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
import java.util.concurrent.ConcurrentHashMap;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.MarkedReference;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerChannel;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;

public class ChannelTree {

  private final ConcurrentHashMap<String, ChannelTree> childMap = new ConcurrentHashMap<>();
  private final ChannelTree parent;
  private final OumuamuaServerChannel serverChannel;

  public ChannelTree () {

    this(null, null);
  }

  public ChannelTree (ChannelTree parent, OumuamuaServerChannel serverChannel) {

    this.parent = parent;
    this.serverChannel = serverChannel;
  }

  public OumuamuaServerChannel getServerChannel () {

    return serverChannel;
  }

  public ChannelTree find (int index, ChannelId channelId) {

    ChannelTree child;

    if ((child = childMap.get(channelId.getSegment(index))) == null) {

      return null;
    } else {

      return (index == (channelId.depth() - 1)) ? child : find(index + 1, channelId);
    }
  }

  public MarkedReference<ChannelTree> createIfAbsent (OumuamuaServer oumuamuaServer, int index, ChannelId channelId) {

    ChannelTree child;
    boolean created = false;

    if ((child = childMap.get(channelId.getSegment(index))) == null) {
      synchronized (this) {
        if ((child = childMap.get(channelId.getSegment(index))) == null) {

          ChannelId branchChannelId = (index == (channelId.depth() - 1)) ? channelId : ChannelIdUtility.from(index + 1, channelId);

          created = true;
          childMap.put(channelId.getSegment(index), child = new ChannelTree(this, new OumuamuaServerChannel(oumuamuaServer, branchChannelId)));
        }
      }
    }

    return (index == (channelId.depth() - 1)) ? new MarkedReference<>(child, created) : child.createIfAbsent(oumuamuaServer, index + 1, channelId);
  }

  public ChannelTree remove (int index, ChannelId channelId) {

    if ((index == (channelId.depth() - 1))) {

      return childMap.remove(channelId.getSegment(index));
    } else {

      ChannelTree child;

      return ((child = childMap.get(channelId.getSegment(index))) == null) ? null : child.remove(index + 1, channelId);
    }
  }

  public void publish (ChannelIterator channelIterator, OumuamuaPacket packet, HashSet<String> sessionIdSet) {

    if (channelIterator.hasNext()) {

      ChannelTree deepWildBranch;
      ChannelTree nextBranch;

      if ((deepWildBranch = childMap.get(ChannelId.DEEPWILD)) != null) {
        deepWildBranch.getServerChannel().send(packet, sessionIdSet);
      }
      if ((nextBranch = childMap.get(channelIterator.next())) != null) {
        nextBranch.publish(channelIterator, packet, sessionIdSet);
      }
    } else {
      if (parent != null) {

        ChannelTree wildBranch;

        if ((wildBranch = parent.childMap.get(ChannelId.WILD)) != null) {
          wildBranch.getServerChannel().send(packet, sessionIdSet);
        }
      }

      getServerChannel().send(packet, sessionIdSet);
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
