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
package org.smallmind.bayeux.oumuamua.server.spi.extension;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;
import org.smallmind.scribe.pen.LoggerManager;

public class AckExtension<V extends Value<V>> extends AbstractServerPacketListener<V> {

  private static final String ACK_FLAG_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.flag";
  private static final String ACK_COUNTER_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.counter";
  private static final String ACK_SIZE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.size";
  private static final String ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.unacknowledged_map";
  private static final String ACK_RESEND_QUEUE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.resend_queue";
  private final int maxAckQueueSize;

  public AckExtension (int maxAckQueueSize) {

    this.maxAckQueueSize = maxAckQueueSize;
  }

  @Override
  public Packet<V> onRequest (final Session<V> sender, Packet<V> packet) {

    if (sender != null) {
      if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute())) {
        for (Message<V> message : packet.getMessages()) {

          ObjectValue<V> extValue;

          if ((extValue = message.getExt()) != null) {

            Value<V> ackValue;

            if (((ackValue = extValue.get("ack")) != null) && ValueType.BOOLEAN.equals(ackValue.getType()) && ((BooleanValue<V>)ackValue).asBoolean()) {
              synchronized (sender) {
                if (!Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {
                  sender.setAttribute(ACK_FLAG_ATTRIBUTE, Boolean.TRUE);
                  sender.setAttribute(ACK_COUNTER_ATTRIBUTE, new AtomicLong(0));
                  sender.setAttribute(ACK_SIZE_ATTRIBUTE, new AtomicLong(0));
                  sender.setAttribute(ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE, new ConcurrentSkipListMap<Long, Packet<V>>());
                  sender.setAttribute(ACK_RESEND_QUEUE_ATTRIBUTE, new ConcurrentLinkedQueue<Packet<V>>());
                }
              }
              break;
            }
          }
        }
      } else if (Meta.CONNECT.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {

          Long ackId = null;

          for (Message<V> message : packet.getMessages()) {

            ObjectValue<V> extValue;

            if ((extValue = message.getExt()) != null) {

              Value<V> ackValue;

              if (((ackValue = extValue.get("ack")) != null) && ValueType.NUMBER.equals(ackValue.getType())) {
                if ((ackId == null) || (ackId < ((NumberValue<V>)ackValue).asLong())) {
                  ackId = ((NumberValue<V>)ackValue).asLong();
                }
              }
            }
          }

          if (ackId != null) {

            ConcurrentSkipListMap<Long, Packet<V>> unacknowledgedMap = (ConcurrentSkipListMap<Long, Packet<V>>)sender.getAttribute(ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE);
            ConcurrentLinkedQueue<Packet<V>> resendQueue = (ConcurrentLinkedQueue<Packet<V>>)sender.getAttribute(ACK_RESEND_QUEUE_ATTRIBUTE);
            AtomicLong ackSize = (AtomicLong)sender.getAttribute(ACK_SIZE_ATTRIBUTE);
            Iterator<Map.Entry<Long, Packet<V>>> unAckedIter;

            unacknowledgedMap.remove(ackId);
            ackSize.decrementAndGet();

            unAckedIter = unacknowledgedMap.headMap(ackId).entrySet().iterator();
            while (unAckedIter.hasNext()) {
              resendQueue.add(packet);
              unAckedIter.remove();
            }
          }
        }
      }
    }

    return packet;
  }

  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    if (sender != null) {
      if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {
          sender.setLongPolling(true);

          for (Message<V> message : packet.getMessages()) {
            if (message.isSuccessful()) {
              message.getExt(true).put("ack", true);
            }
          }
        }
      } else if (Meta.CONNECT.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE)) && (packet.getMessages().length > 1)) {

          Iterator<Packet<V>> resendIter;
          Long ackId = null;

          for (Message<V> message : packet.getMessages()) {
            if (message.isSuccessful() && Meta.CONNECT.getRoute().getPath().equals(message.getChannel())) {
              if (ackId == null) {
                ackId = ((AtomicLong)sender.getAttribute(ACK_COUNTER_ATTRIBUTE)).incrementAndGet();
              }

              message.getExt(true).put("ack", ackId);
            }
          }

          resendIter = ((ConcurrentLinkedQueue<Packet<V>>)sender.getAttribute(ACK_RESEND_QUEUE_ATTRIBUTE)).iterator();
          if (resendIter.hasNext()) {
            while (resendIter.hasNext()) {
              packet = PacketUtility.merge(packet, resendIter.next());
              resendIter.remove();
            }
          }

          if (ackId != null) {

            ConcurrentSkipListMap<Long, Packet<V>> unacknowledgedMap = (ConcurrentSkipListMap<Long, Packet<V>>)sender.getAttribute(ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE);
            AtomicLong ackSize = (AtomicLong)sender.getAttribute(ACK_SIZE_ATTRIBUTE);

            if (ackSize.incrementAndGet() > maxAckQueueSize) {
              LoggerManager.getLogger(AckExtension.class).debug("Session(%s) overflowed the ack queue", sender.getId());

              if (unacknowledgedMap.pollLastEntry() != null) {
                ackSize.decrementAndGet();
              }
            }

            unacknowledgedMap.put(ackId, packet);
          }
        }
      }
    }

    return packet;
  }
}
