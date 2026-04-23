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
package org.smallmind.bayeux.oumuamua.server.spi.latency;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.HistogramBuilder;
import org.smallmind.claxon.registry.meter.MeterFactory;

/**
 * {@link Protocol.ProtocolListener} that instruments end-to-end message latency by stamping each
 * inbound message with a server receipt timestamp and, at delivery time, recording the elapsed
 * duration as a Claxon histogram metric tagged with host name, delivery type, and whether the
 * packet originated from a remote backbone node.
 *
 * @param <V> the concrete {@link Value} type carried by messages in this deployment
 */
public class InstrumentedLatencyListener<V extends Value<V>> implements Protocol.ProtocolListener<V> {

  private final String hostName;

  /**
   * Constructs the listener and resolves the local host name used to tag all emitted metrics.
   *
   * @throws UnknownHostException if the local hostname cannot be resolved by the JVM
   */
  public InstrumentedLatencyListener ()
    throws UnknownHostException {

    InetAddress localHost = InetAddress.getLocalHost();

    hostName = localHost.getHostName();
  }

  /**
   * Writes a {@code ext.latency.timestamp} field containing the current server wall-clock time into
   * every message, establishing the start of the latency measurement window.
   *
   * @param incomingMessages the messages just received from the transport layer
   */
  @Override
  public void onReceipt (Message<V>[] incomingMessages) {

    long now = System.currentTimeMillis();

    for (Message<V> message : incomingMessages) {

      ObjectValue<V> latencyValue = message.getFactory().objectValue();

      message.getExt(true).put("latency", latencyValue.put("timestamp", now));
    }
  }

  /**
   * Propagates the {@code ext.latency} object from the originating message to the outgoing fan-out
   * message so that the receipt timestamp is available when delivery latency is recorded.
   *
   * @param originatingMessage the original client message carrying the latency timestamp
   * @param outgoingMessage    the fan-out copy being dispatched to a subscriber
   */
  @Override
  public void onPublish (Message<V> originatingMessage, Message<V> outgoingMessage) {

    ObjectValue<V> extValue;

    if ((extValue = originatingMessage.getExt()) != null) {

      Value<V> latencyValue;

      if (((latencyValue = extValue.get("latency")) != null) && ValueType.OBJECT.equals(latencyValue.getType())) {
        outgoingMessage.getExt(true).put("latency", latencyValue);
      }
    }
  }

  /**
   * Calculates the elapsed time since receipt for each message that carries a latency timestamp and
   * records the value (in milliseconds, offset by one to avoid zero-bucket issues) in a Claxon
   * histogram tagged with {@code host}, {@code delivery}, and {@code remote}.
   *
   * @param outgoingPacket the packet about to be written to the transport
   */
  @Override
  public void onDelivery (Packet<V> outgoingPacket) {

    long now = System.currentTimeMillis();

    for (Message<V> message : outgoingPacket.getMessages()) {

      ObjectValue<V> extValue;

      if ((extValue = message.getExt()) != null) {

        Value<V> latencyValue;

        if (((latencyValue = extValue.get("latency")) != null) && ValueType.OBJECT.equals(latencyValue.getType())) {

          Value<V> timestampValue;

          if (((timestampValue = ((ObjectValue<V>)latencyValue).get("timestamp")) != null) && ValueType.NUMBER.equals(timestampValue.getType())) {

            long timeInTransit = now - ((NumberValue<V>)timestampValue).asLong();

            if (timeInTransit >= 0) {

              Instrument.with(InstrumentedLatencyListener.class, MeterFactory.instance(HistogramBuilder::new), new Tag("host", hostName), new Tag("delivery", Boolean.toString(PacketType.DELIVERY.equals(outgoingPacket.getPacketType()))), new Tag("remote", Boolean.toString(isRemote(extValue)))).update(timeInTransit + 1);
            }
          }
        }
      }
    }
  }

  /**
   * Inspects {@code ext.backbone.remote} to determine whether the packet arrived via a backbone
   * relay from another cluster node.
   *
   * @param extValue the {@code ext} object of the message being evaluated
   * @return {@code true} if {@code ext.backbone.remote} is present and {@code true}
   */
  private boolean isRemote (ObjectValue<V> extValue) {

    Value<V> backboneValue;

    if (((backboneValue = extValue.get("backbone")) != null) && ValueType.OBJECT.equals(backboneValue.getType())) {

      Value<V> remoteValue;

      return ((remoteValue = ((ObjectValue<V>)backboneValue).get("remote")) != null) && ValueType.BOOLEAN.equals(remoteValue.getType()) && ((BooleanValue<V>)remoteValue).asBoolean();
    }

    return false;
  }
}
