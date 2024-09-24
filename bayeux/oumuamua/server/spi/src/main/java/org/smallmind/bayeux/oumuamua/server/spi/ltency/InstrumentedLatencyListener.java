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
package org.smallmind.bayeux.oumuamua.server.spi.ltency;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
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

public class InstrumentedLatencyListener<V extends Value<V>> implements Protocol.ProtocolListener<V> {

  @Override
  public void onReceipt (Message<V>[] incomingMessages) {

    long now = System.nanoTime();

    for (Message<V> message : incomingMessages) {

      ObjectValue<V> latencyValue = message.getFactory().objectValue();

      message.getExt(true).put("latency", latencyValue.put("timestamp", now));
    }
  }

  @Override
  public void onDelivery (Packet<V> outgoingPacket) {

    long now = System.nanoTime();

    for (Message<V> message : outgoingPacket.getMessages()) {

      ObjectValue<V> extValue;

      if ((extValue = message.getExt()) != null) {

        Value<V> latencyValue;

        if (((latencyValue = extValue.get("latency")) != null) && ValueType.OBJECT.equals(latencyValue.getType())) {

          Value<V> timestampValue;

          if (((timestampValue = ((ObjectValue<V>)latencyValue).get("timestamp")) != null) && ValueType.NUMBER.equals(timestampValue.getType())) {

            long timeInTransit = now - ((NumberValue<V>)timestampValue).asLong();

            Instrument.with(InstrumentedLatencyListener.class, HistogramBuilder.instance(), new Tag("remote", Boolean.toString(isRemote(extValue)))).update(timeInTransit);
          }
        }
      }
    }
  }

  private boolean isRemote (ObjectValue<V> extValue) {

    Value<V> backboneValue;

    if (((backboneValue = extValue.get("backbone")) != null) && ValueType.OBJECT.equals(backboneValue.getType())) {

      Value<V> remoteValue;

      return ((remoteValue = ((ObjectValue<V>)backboneValue).get("remote")) != null) && ValueType.BOOLEAN.equals(remoteValue.getType()) && ((BooleanValue<V>)remoteValue).asBoolean();
    }

    return false;
  }
}
