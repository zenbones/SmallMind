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
package org.smallmind.cometd.oumuamua.meta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;
import org.smallmind.web.json.scaffold.util.JsonCodec;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class ConnectMessage extends AdvisedMetaMessage {

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String clientId;
  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private String connectionType;

  public String process (OumuamuaServer oumuamuaServer, OumuamuaServerSession serverSession)
    throws JsonProcessingException {

    ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

    if ((serverSession == null) || (!serverSession.getId().equals(getClientId()))) {
      adviceNode.put("reconnect", "handshake");

      return JsonCodec.writeAsString(new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel("/meta/connect").setId(getId()).setError("Handshake required").setAdvice(adviceNode));
    } else if (!serverSession.isHandshook()) {
      adviceNode.put("reconnect", "handshake");

      return JsonCodec.writeAsString(new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel("/meta/connect").setId(getId()).setClientId(serverSession.getId()).setError("Handshake required").setAdvice(adviceNode));
    } else {
      for (String allowedTransport : oumuamuaServer.getAllowedTransports()) {
        if (allowedTransport.equalsIgnoreCase(connectionType)) {

          StringBuilder batchedResponseBuilder = null;
          String connectResponseText;
          MapLike enqueuedMapLile;

          serverSession.setConnected(true);
          adviceNode.put("interval", 30000);

          while ((enqueuedMapLile = serverSession.poll()) != null) {
            if (batchedResponseBuilder == null) {
              batchedResponseBuilder = new StringBuilder();
            }

            batchedResponseBuilder.append(',').append(enqueuedMapLile.encode());
          }

          connectResponseText = JsonCodec.writeAsString(new ConnectMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel("/meta/connect").setId(getId()).setClientId(serverSession.getId()).setAdvice(adviceNode));

          return (batchedResponseBuilder == null) ? connectResponseText : batchedResponseBuilder.insert(0, connectResponseText).toString();
        }
      }

      adviceNode.put("reconnect", "handshake");

      return JsonCodec.writeAsString(new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel("/meta/connect").setId(getId()).setClientId(serverSession.getId()).setError("Unavailable transport").setAdvice(adviceNode));
    }
  }

  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  public String getConnectionType () {

    return connectionType;
  }

  public void setConnectionType (String connectionType) {

    this.connectionType = connectionType;
  }
}
