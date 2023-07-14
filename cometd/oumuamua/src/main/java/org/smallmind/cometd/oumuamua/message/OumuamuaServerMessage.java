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
package org.smallmind.cometd.oumuamua.message;

import java.util.Map;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public class OumuamuaServerMessage extends MapLike implements ServerMessage {

  private final OumuamuaTransport transport;
  private final BayeuxContext context;
  private final Mutable associated;
  private final ChannelId channelId;
  private final String id;
  private final String clientId;
  private final boolean lazy;

  public OumuamuaServerMessage (OumuamuaTransport transport, BayeuxContext context, Mutable associated, ChannelId channelId, String id, String clientId, boolean lazy, ObjectNode node) {

    super(null, node);

    this.transport = transport;
    this.context = context;
    this.associated = associated;
    this.channelId = channelId;
    this.id = id;
    this.clientId = clientId;
    this.lazy = lazy;
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public String getClientId () {

    return clientId;
  }

  @Override
  public String getChannel () {

    return channelId.getId();
  }

  @Override
  public ChannelId getChannelId () {

    return channelId;
  }

  @Override
  public boolean isMeta () {

    return channelId.isMeta();
  }

  @Override
  public boolean isPublishReply () {

    return !(channelId.isMeta() || containsKey(DATA_FIELD));
  }

  @Override
  public boolean isLazy () {

    return lazy;
  }

  @Override
  public Mutable getAssociated () {

    return associated;
  }

  @Override
  public boolean isSuccessful () {

    return Boolean.TRUE.equals(get(SUCCESSFUL_FIELD));
  }

  @Override
  public BayeuxContext getBayeuxContext () {

    return context;
  }

  @Override
  public ServerTransport getServerTransport () {

    return transport;
  }

  @Override
  public Object getData () {

    return get(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getDataAsMap () {

    return getAsMapLike(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getAdvice () {

    return getAsMapLike(ADVICE_FIELD);
  }

  @Override
  public Map<String, Object> getExt () {

    return getAsMapLike(EXT_FIELD);
  }
}
