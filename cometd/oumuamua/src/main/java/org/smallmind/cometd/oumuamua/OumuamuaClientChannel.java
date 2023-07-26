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
package org.smallmind.cometd.oumuamua;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;

public class OumuamuaClientChannel implements ClientSessionChannel {

  private final ReentrantLock releaseLock = new ReentrantLock();
  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<MessageListener> subscriptionList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ClientSessionChannelListener> listenerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaClientSession clientSession;
  private final ChannelId channelId;
  private boolean released;

  public OumuamuaClientChannel (OumuamuaClientSession clientSession, ChannelId channelId) {

    this.clientSession = clientSession;
    this.channelId = channelId;
  }

  @Override
  public String getId () {

    return channelId.getId();
  }

  @Override
  public ChannelId getChannelId () {

    return channelId;
  }

  @Override
  public boolean isMeta () {

    return channelId.isMeta();
  }

  @Override
  public boolean isService () {

    return channelId.isService();
  }

  @Override
  public boolean isBroadcast () {

    return !(isMeta() || isService());
  }

  @Override
  public boolean isWild () {

    return channelId.isWild();
  }

  @Override
  public boolean isDeepWild () {

    return channelId.isDeepWild();
  }

  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }

  @Override
  public void addListener (ClientSessionChannelListener listener) {

    releaseLock.lock();

    try {
      listenerList.add(listener);
    } finally {
      releaseLock.unlock();
    }
  }

  @Override
  public void removeListener (ClientSessionChannelListener listener) {

    releaseLock.lock();

    try {
      listenerList.remove(listener);
    } finally {
      releaseLock.unlock();
    }
  }

  @Override
  public List<ClientSessionChannelListener> getListeners () {

    return new LinkedList<>(listenerList);
  }

  @Override
  public ClientSession getSession () {

    return clientSession;
  }

  @Override
  public void publish (Object data, ClientSession.MessageListener callback) {

  }

  @Override
  public void publish (Message.Mutable message, ClientSession.MessageListener callback) {

  }

  @Override
  public boolean subscribe (Message.Mutable message, MessageListener listener, ClientSession.MessageListener callback) {

    releaseLock.lock();

    try {

    } finally {
      releaseLock.unlock();
    }

    return false;
  }

  @Override
  public boolean unsubscribe (Message.Mutable message, MessageListener listener, ClientSession.MessageListener callback) {

    releaseLock.lock();

    try {

    } finally {
      releaseLock.unlock();
    }

    return false;
  }

  @Override
  public void unsubscribe () {

    releaseLock.lock();

    try {
      subscriptionList.clear();
    } finally {
      releaseLock.unlock();
    }
  }

  @Override
  public List<MessageListener> getSubscribers () {

    return new LinkedList<>(subscriptionList);
  }

  @Override
  public boolean release () {

    releaseLock.lock();

    try {
      if (listenerList.isEmpty() && subscriptionList.isEmpty()) {
        clientSession.release(channelId.getId());
        released = true;
      }

      return released;
    } finally {
      releaseLock.unlock();
    }
  }

  @Override
  public boolean isReleased () {

    releaseLock.lock();

    try {

      return released;
    } finally {
      releaseLock.unlock();
    }
  }
}
