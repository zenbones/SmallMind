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
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public class OumuamuaMutableServerMessage extends OumuamuaServerMessage implements ServerMessage.Mutable {

  private Mutable associated;
  private ChannelId channelId;
  private String id;
  private String clientId;
  private Boolean lazy;

  public OumuamuaMutableServerMessage (OumuamuaTransport transport, BayeuxContext context, Mutable associated, ChannelId channelId, String id, String clientId, boolean lazy, ObjectNode node) {

    super(transport, context, associated, channelId, id, clientId, lazy, node);
  }

  @Override
  public String getId () {

    return (id == null) ? super.getId() : id;
  }

  @Override
  public void setId (String id) {

    this.id = id;
  }

  @Override
  public String getClientId () {

    return (clientId == null) ? super.getClientId() : clientId;
  }

  @Override
  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  @Override
  public ChannelId getChannelId () {

    return (channelId == null) ? super.getChannelId() : channelId;
  }

  @Override
  public void setChannel (String channel) {

    channelId = ChannelIdCache.generate(channel);
  }

  @Override
  public boolean isLazy () {

    return (lazy == null) ? super.isLazy() : lazy;
  }

  @Override
  public void setLazy (boolean lazy) {

    this.lazy = lazy;
  }

  @Override
  public Mutable getAssociated () {

    return (associated == null) ? super.getAssociated() : associated;
  }

  @Override
  public void setAssociated (Mutable associated) {

    this.associated = associated;
  }

  @Override
  public void setSuccessful (boolean successful) {

    put(SUCCESSFUL_FIELD, successful);
  }

  @Override
  public void setData (Object data) {

    put(DATA_FIELD, data);
  }

  @Override
  public Map<String, Object> getDataAsMap (boolean create) {

    return create ? createIfAbsentMapLike(DATA_FIELD) : getAsMapLike(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getAdvice (boolean create) {

    return create ? createIfAbsentMapLike(ADVICE_FIELD) : getAsMapLike(ADVICE_FIELD);
  }

  @Override
  public Map<String, Object> getExt (boolean create) {

    return create ? createIfAbsentMapLike(EXT_FIELD) : getAsMapLike(EXT_FIELD);
  }
}
