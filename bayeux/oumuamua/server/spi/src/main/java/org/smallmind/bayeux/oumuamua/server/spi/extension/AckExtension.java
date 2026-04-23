/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Server-side implementation of the Bayeux {@code ack} extension that guarantees at-least-once
 * delivery by tracking unacknowledged packets per session and re-queuing them until the client
 * confirms receipt via an ack identifier on subsequent connect responses.
 *
 * @param <V> the concrete {@link Value} type carried by messages in this deployment
 */
public class AckExtension<V extends Value<V>> extends AbstractServerPacketListener<V> {

  private static final String ACK_FLAG_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.flag";
  private static final String ACK_COUNTER_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.counter";
  private static final String ACK_SIZE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.size";
  private static final String ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.unacknowledged_map";
  private static final String ACK_RESEND_QUEUE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.resend_queue";
  private final Level overflowLogLevel;
  private final int maxAckQueueSize;

  /**
   * Creates the extension with the given maximum unacknowledged-message queue capacity and
   * {@link Level#DEBUG} as the overflow log level.
   *
   * @param maxAckQueueSize upper bound on the total number of messages that may be held in the
   *                        unacknowledged map across all in-flight packets for a single session
   */
  public AckExtension (int maxAckQueueSize) {

    this(maxAckQueueSize, Level.DEBUG);
  }

  /**
   * Creates the extension with a custom overflow log level.
   *
   * @param maxAckQueueSize  upper bound on the total number of messages that may be held in the
   *                         unacknowledged map for a single session
   * @param overflowLogLevel level at which a log entry is emitted when the unacknowledged map is
   *                         trimmed due to overflow; pass {@code null} to suppress overflow logging
   */
  public AckExtension (int maxAckQueueSize, Level overflowLogLevel) {

    this.maxAckQueueSize = maxAckQueueSize;
    this.overflowLogLevel = (overflowLogLevel == null) ? Level.OFF : overflowLogLevel;
  }

  /**
   * Initialises per-session ack state on handshake when the client advertises {@code ext.ack=true},
   * and on connect advances the unacknowledged map by removing entries whose ack id has been
   * confirmed and moving any entries with lower ids to the resend queue.
   *
   * @param sender the session submitting the request, or {@code null} for anonymous requests
   * @param packet the inbound request packet to inspect and pass through
   * @return {@code packet} unchanged
   */
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
            Iterator<Map.Entry<Long, Packet<V>>> unackedIter;
            Packet<V> unacknowledgedPacket;

            if ((unacknowledgedPacket = unacknowledgedMap.remove(ackId)) != null) {
              ackSize.accumulateAndGet(unacknowledgedPacket.getMessages().length, (x, y) -> x - y);
            }

            unackedIter = unacknowledgedMap.headMap(ackId).entrySet().iterator();
            while (unackedIter.hasNext()) {
              resendQueue.add(unacknowledgedPacket = unackedIter.next().getValue());
              unackedIter.remove();
              ackSize.accumulateAndGet(unacknowledgedPacket.getMessages().length, (x, y) -> x - y);
            }
          }
        }
      }
    }

    return packet;
  }

  /**
   * Annotates handshake responses with {@code ext.ack=true} to confirm extension support, and on
   * connect prepends any pending resend-queue packets to the response, stamps a new ack id onto the
   * connect message, records the merged packet in the unacknowledged map, and trims the map when its
   * accumulated message count exceeds {@code maxAckQueueSize}.
   *
   * @param sender the session the response is being sent to, or {@code null} for anonymous sessions
   * @param packet the outbound response packet, which may be replaced by a merged packet
   * @return the response packet, potentially merged with resent packets
   */
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
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE)) && (packet.getMessages() != null) && (packet.getMessages().length > 0)) {

          Iterator<Packet<V>> resendIter;

          resendIter = ((ConcurrentLinkedQueue<Packet<V>>)sender.getAttribute(ACK_RESEND_QUEUE_ATTRIBUTE)).iterator();
          if (resendIter.hasNext()) {
            while (resendIter.hasNext()) {
              packet = PacketUtility.merge(packet, resendIter.next(), Meta.CONNECT.getRoute(), true);
              resendIter.remove();
            }
          }

          if (packet.getMessages().length > 1) {

            Long ackId = null;

            for (Message<V> message : packet.getMessages()) {
              if (message.isSuccessful() && Meta.CONNECT.getRoute().getPath().equals(message.getChannel())) {
                ackId = ((AtomicLong)sender.getAttribute(ACK_COUNTER_ATTRIBUTE)).incrementAndGet();
                message.getExt(true).put("ack", ackId);
                break;
              }
            }

            if (ackId != null) {

              ConcurrentSkipListMap<Long, Packet<V>> unacknowledgedMap = (ConcurrentSkipListMap<Long, Packet<V>>)sender.getAttribute(ACK_UNACKNOWLEDGED_MAP_ATTRIBUTE);
              AtomicLong ackSize = (AtomicLong)sender.getAttribute(ACK_SIZE_ATTRIBUTE);
              long accumulatedSize;

              if ((accumulatedSize = ackSize.accumulateAndGet(packet.getMessages().length, Long::sum)) > maxAckQueueSize) {

                Map.Entry<Long, Packet<V>> unacknowledgedEntry;

                LoggerManager.getLogger(AckExtension.class).log(overflowLogLevel, "Session(%s) overflowed the ack queue", sender.getId());

                do {
                  if ((unacknowledgedEntry = unacknowledgedMap.pollLastEntry()) != null) {
                    accumulatedSize = ackSize.accumulateAndGet(unacknowledgedEntry.getValue().getMessages().length, (x, y) -> x - y);
                  }
                } while ((unacknowledgedEntry != null) && (accumulatedSize > maxAckQueueSize));
              }

              unacknowledgedMap.put(ackId, packet);
            }
          }
        }
      }
    }

    return packet;
  }
}
