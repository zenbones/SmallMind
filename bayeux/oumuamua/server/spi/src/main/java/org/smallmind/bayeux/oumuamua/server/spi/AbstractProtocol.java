/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Base {@link Protocol} implementation that dispatches lifecycle events to registered listeners.
 *
 * @param <V> concrete value type used in messages
 */
public abstract class AbstractProtocol<V extends Value<V>> implements Protocol<V> {

  private final ConcurrentLinkedQueue<Listener<V>> listenerList = new ConcurrentLinkedQueue<>();

  /**
   * Notifies protocol listeners of received messages.
   *
   * @param incomingMessages messages received from a client
   */
  public void onReceipt (Message<V>[] incomingMessages) {

    for (Listener<V> listener : listenerList) {
      if (ProtocolListener.class.isAssignableFrom(listener.getClass())) {
        ((ProtocolListener<V>)listener).onReceipt(incomingMessages);
      }
    }
  }

  /**
   * Notifies listeners when a message is published.
   *
   * @param originatingMessage message supplied by the client
   * @param outgoingMessage message produced for delivery
   */
  public void onPublish (Message<V> originatingMessage, Message<V> outgoingMessage) {

    for (Listener<V> listener : listenerList) {
      if (ProtocolListener.class.isAssignableFrom(listener.getClass())) {
        ((ProtocolListener<V>)listener).onPublish(originatingMessage, outgoingMessage);
      }
    }
  }

  /**
   * Notifies listeners when a packet is delivered.
   *
   * @param outgoingPacket the packet to be delivered
   */
  public void onDelivery (Packet<V> outgoingPacket) {

    for (Listener<V> listener : listenerList) {
      if (ProtocolListener.class.isAssignableFrom(listener.getClass())) {
        ((ProtocolListener<V>)listener).onDelivery(outgoingPacket);
      }
    }
  }

  /**
   * Adds a listener to the protocol.
   *
   * @param listener listener to add
   */
  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  /**
   * Removes a listener from the protocol.
   *
   * @param listener listener to remove
   */
  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }
}
