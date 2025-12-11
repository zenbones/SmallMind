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
package org.smallmind.bayeux.oumuamua.server.spi.extension;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

/**
 * Implements the Bayeux timesync extension for latency/offset calculation between client and server.
 *
 * @param <V> concrete value type used in messages
 */
public class TimesyncExtension<V extends Value<V>> extends AbstractServerPacketListener<V> {

  private static final String TIME_SYNC_VALUE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.timesync.value";

  /**
   * Records timing data supplied by the client on handshake/connect requests.
   *
   * @param sender originating session
   * @param packet incoming packet
   * @return the original packet
   */
  @Override
  public Packet<V> onRequest (Session<V> sender, Packet<V> packet) {

    if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute()) || Meta.CONNECT.getRoute().equals(packet.getRoute())) {
      for (Message<V> message : packet.getMessages()) {

        String messageId;

        if ((messageId = message.getId()) != null) {

          ObjectValue<V> extValue;

          if ((extValue = message.getExt()) != null) {

            Value<V> timesycValue;

            if (((timesycValue = extValue.get("timesync")) != null) && ValueType.OBJECT.equals(timesycValue.getType())) {

              Value<V> tcValue = ((ObjectValue<V>)timesycValue).get("tc");
              Value<V> lValue = ((ObjectValue<V>)timesycValue).get("l");
              Value<V> oValue = ((ObjectValue<V>)timesycValue).get("o");

              if ((tcValue != null) && ValueType.NUMBER.equals(tcValue.getType()) && (lValue != null) && ValueType.NUMBER.equals(lValue.getType()) && (oValue != null) && ValueType.NUMBER.equals(oValue.getType())) {

                TimeSync timeSync;

                if (((timeSync = (TimeSync)sender.getAttribute(TIME_SYNC_VALUE_ATTRIBUTE)) == null) || (timeSync.getTc() <= ((NumberValue<V>)tcValue).asLong())) {
                  sender.setAttribute(TIME_SYNC_VALUE_ATTRIBUTE, new TimeSync(messageId, ((NumberValue<V>)tcValue).asLong(), ((NumberValue<V>)lValue).asLong(), ((NumberValue<V>)oValue).asLong()));
                }
              }
              break;
            }
          }
        }
      }
    }

    return packet;
  }

  /**
   * Adds timesync response data for the matching request message.
   *
   * @param sender originating session
   * @param packet outgoing packet
   * @return the original packet
   */
  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute()) || Meta.CONNECT.getRoute().equals(packet.getRoute())) {

      TimeSync timeSync;

      if ((timeSync = (TimeSync)sender.getAttribute(TIME_SYNC_VALUE_ATTRIBUTE)) != null) {
        for (Message<V> message : packet.getMessages()) {

          String messageId;

          if (((messageId = message.getId()) != null) && timeSync.getId().equals(messageId)) {

            ObjectValue<V> extValue = message.getExt(true);
            ObjectValue<V> timesyncValue = message.getFactory().objectValue();

            timesyncValue.put("tc", timeSync.getTc());
            timesyncValue.put("ts", timeSync.getTs());
            timesyncValue.put("p", System.currentTimeMillis() - timeSync.getTs());
            timesyncValue.put("a", timeSync.getTc() + timeSync.getO() + timeSync.getL() - timeSync.getTs());

            extValue.put("timesync", timesyncValue);
            break;
          }
        }
      }
    }

    return packet;
  }

  /**
   * Captures timing metrics sent by the client.
   */
  private static class TimeSync {

    private final String id;
    private final long ts;
    private final long tc;
    private final long l;
    private final long o;

    /**
     * Captures a measurement based on client supplied values and the current server timestamp.
     *
     * @param id message identifier
     * @param tc client timestamp
     * @param l  network latency estimate
     * @param o  client clock offset
     */
    public TimeSync (String id, long tc, long l, long o) {

      ts = System.currentTimeMillis();

      this.id = id;
      this.tc = tc;
      this.l = l;
      this.o = o;
    }

    /**
     * @return associated message id
     */
    public String getId () {

      return id;
    }

    /**
     * @return server timestamp recorded when the message was received
     */
    public long getTs () {

      return ts;
    }

    /**
     * @return client timestamp
     */
    public long getTc () {

      return tc;
    }

    /**
     * @return estimated network latency
     */
    public long getL () {

      return l;
    }

    /**
     * @return client clock offset
     */
    public long getO () {

      return o;
    }
  }
}
