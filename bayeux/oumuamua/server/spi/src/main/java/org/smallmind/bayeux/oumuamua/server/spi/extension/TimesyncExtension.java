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

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

public class TimesyncExtension<V extends Value<V>> extends AbstractServerPacketListener<V> {

  private static final String TIME_SYNC_VALUE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.timesync.value";

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

            break;
          }
        }
      }
    }

    return packet;
  }

  private static class TimeSync {

    private final String id;
    private final long ts;
    private final long tc;
    private final long l;
    private final long o;

    public TimeSync (String id, long tc, long l, long o) {

      ts = System.currentTimeMillis();

      this.id = id;
      this.tc = tc;
      this.l = l;
      this.o = o;
    }

    public String getId () {

      return id;
    }

    public long getTs () {

      return ts;
    }

    public long getTc () {

      return tc;
    }

    public long getL () {

      return l;
    }

    public long getO () {

      return o;
    }
  }
}
