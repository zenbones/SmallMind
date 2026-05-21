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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;

/**
 * Test-only {@link Channel.PacketListener} that vetoes any delivery whose first message has a
 * {@code data} field starting with a configurable veto prefix. The listener records observation
 * and veto counts so a test can confirm it was invoked the expected number of times.
 */
public class VetoingChannelPacketListener implements Channel.PacketListener<OrthodoxValue> {

  private final AtomicInteger observed = new AtomicInteger();
  private final AtomicInteger vetoed = new AtomicInteger();
  private final String vetoPrefix;

  /**
   * Constructs the listener with the prefix used to mark data payloads for veto.
   *
   * @param vetoPrefix data string prefix that causes {@link #onDelivery} to return {@code null}
   */
  public VetoingChannelPacketListener (String vetoPrefix) {

    this.vetoPrefix = vetoPrefix;
  }

  /**
   * Returns the number of {@link #onDelivery} invocations observed since construction.
   *
   * @return observed delivery count
   */
  public int observedCount () {

    return observed.get();
  }

  /**
   * Returns the number of {@link #onDelivery} invocations that resulted in a veto.
   *
   * @return veto count
   */
  public int vetoedCount () {

    return vetoed.get();
  }

  @Override
  public boolean isPersistent () {

    return true;
  }

  @Override
  public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

    observed.incrementAndGet();

    if ((packet.getMessages() != null) && (packet.getMessages().length > 0)) {

      Message<OrthodoxValue> message = packet.getMessages()[0];
      Value<OrthodoxValue> dataValue = message.get(Message.DATA);

      if ((dataValue != null) && ValueType.STRING.equals(dataValue.getType()) && ((StringValue<OrthodoxValue>)dataValue).asText().startsWith(vetoPrefix)) {
        vetoed.incrementAndGet();

        return null;
      }
    }

    return packet;
  }
}
