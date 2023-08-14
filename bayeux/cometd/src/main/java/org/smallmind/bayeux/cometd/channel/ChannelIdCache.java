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
package org.smallmind.bayeux.cometd.channel;

import java.util.HashMap;
import org.cometd.bayeux.ChannelId;
import org.smallmind.bayeux.cometd.meta.ConnectMessage;
import org.smallmind.bayeux.cometd.meta.HandshakeMessage;
import org.smallmind.bayeux.cometd.meta.SubscribeMessage;
import org.smallmind.bayeux.cometd.meta.UnsubscribeMessage;

public class ChannelIdCache {

  private static final ThreadLocal<HashMap<String, ChannelId>> CHANNEL_ID_MAP_LOCAL = new ThreadLocal<>() {

    @Override
    protected HashMap<String, ChannelId> initialValue () {

      return new HashMap<>();
    }
  };

  public static ChannelId generate (String id) {

    if (id == null) {
      throw new NullPointerException();
    } else {

      switch (id) {
        case "/meta/handshake":

          return HandshakeMessage.CHANNEL_ID;
        case "/meta/connect":

          return ConnectMessage.CHANNEL_ID;
        case "/meta/subscribe":

          return SubscribeMessage.CHANNEL_ID;
        case "/meta/unsubscribe":

          return UnsubscribeMessage.CHANNEL_ID;
        default:

          ChannelId channelId;

          if ((channelId = CHANNEL_ID_MAP_LOCAL.get().get(id)) == null) {
            CHANNEL_ID_MAP_LOCAL.get().put(id, channelId = new ChannelId(id));
          }

          return channelId;
      }
    }
  }

  public static void clear () {

    CHANNEL_ID_MAP_LOCAL.remove();
  }
}
