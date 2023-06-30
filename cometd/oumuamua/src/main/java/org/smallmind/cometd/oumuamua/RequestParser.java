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

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import org.smallmind.cometd.oumuamua.message.ConnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.message.DisconnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.message.HandshakeMessageRequestInView;
import org.smallmind.cometd.oumuamua.message.PublishMessageRequestInView;
import org.smallmind.cometd.oumuamua.message.SubscribeMessageRequestInView;
import org.smallmind.cometd.oumuamua.message.UnsubscribeMessageRequestInView;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class RequestParser {

  public static void parse (String data)
    throws IOException {

    JsonNode messageNode = JsonCodec.readAsJsonNode(data);

    if (messageNode.has("channel")) {

      String channel;

      switch (channel = messageNode.get("channel").asText()) {
        case "/meta/handshake":
          JsonCodec.convert(messageNode, HandshakeMessageRequestInView.class);
          break;
        case "/meta/subscribe":
          JsonCodec.convert(messageNode, SubscribeMessageRequestInView.class);
          break;
        case "/meta/unsubscribe":
          JsonCodec.convert(messageNode, UnsubscribeMessageRequestInView.class);
          break;
        case "/meta/connect":
          JsonCodec.convert(messageNode, ConnectMessageRequestInView.class);
          break;
        case "/meta/disconnect":
          JsonCodec.convert(messageNode, DisconnectMessageRequestInView.class);
          break;
        case "/meta/publish":
          JsonCodec.convert(messageNode, PublishMessageRequestInView.class);
          break;
        default:
          if (!(channel.startsWith("/meta/") || channel.startsWith("/service"))) {
            // publish???
          }
      }
    }
  }
}
