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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public class OumuamuaServerMessage implements ServerMessage, ServerMessage.Mutable {

  private final MapLike mapLike;
  private final OumuamuaTransport transport;
  private final BayeuxContext context;
  private Mutable associated;
  private ChannelId channelId;
  private boolean lazy;

  public OumuamuaServerMessage (OumuamuaTransport transport, BayeuxContext context, Mutable associated, ChannelId channelId, boolean lazy, MapLike mapLike) {

    this.transport = transport;
    this.context = context;
    this.associated = associated;
    this.channelId = channelId;
    this.lazy = lazy;
    this.mapLike = mapLike;
  }

  @Override
  public String getId () {

    return get(ID_FIELD).toString();
  }

  @Override
  public void setId (String id) {

    put(ID_FIELD, id);
  }

  @Override
  public String getClientId () {

    return get(CLIENT_ID_FIELD).toString();
  }

  @Override
  public void setClientId (String clientId) {

    put(CLIENT_ID_FIELD, clientId);
  }

  @Override
  public String getChannel () {

    return channelId.getId();
  }

  @Override
  public void setChannel (String channel) {

    channelId = ChannelIdCache.generate(channel);
    put(CHANNEL_FIELD, channel);
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
  public void setLazy (boolean lazy) {

    this.lazy = lazy;
  }

  @Override
  public Mutable getAssociated () {

    return associated;
  }

  @Override
  public void setAssociated (Mutable associated) {

    this.associated = associated;
  }

  @Override
  public boolean isSuccessful () {

    return Boolean.TRUE.equals(get(SUCCESSFUL_FIELD));
  }

  @Override
  public void setSuccessful (boolean successful) {

    mapLike.put(SUCCESSFUL_FIELD, successful);
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
  public void setData (Object data) {

    put(DATA_FIELD, data);
  }

  @Override
  public Map<String, Object> getDataAsMap () {

    return mapLike.getAsMapLike(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getDataAsMap (boolean create) {

    return create ? mapLike.createIfAbsentMapLike(DATA_FIELD) : getDataAsMap();
  }

  @Override
  public Map<String, Object> getAdvice () {

    return mapLike.getAsMapLike(ADVICE_FIELD);
  }

  @Override
  public Map<String, Object> getAdvice (boolean create) {

    return create ? mapLike.createIfAbsentMapLike(ADVICE_FIELD) : getAdvice();
  }

  @Override
  public Map<String, Object> getExt () {

    return mapLike.getAsMapLike(EXT_FIELD);
  }

  @Override
  public Map<String, Object> getExt (boolean create) {

    return create ? mapLike.getAsMapLike(EXT_FIELD) : getExt();
  }

  @Override
  public int size () {

    return mapLike.size();
  }

  @Override
  public boolean isEmpty () {

    return mapLike.isEmpty();
  }

  @Override
  public boolean containsKey (Object key) {

    return mapLike.containsKey(key);
  }

  @Override
  public boolean containsValue (Object value) {

    return mapLike.containsValue(value);
  }

  @Override
  public Object get (Object key) {

    return mapLike.get(key);
  }

  @Override
  public Object put (String key, Object value) {

    return mapLike.put(key, value);
  }

  @Override
  public Object remove (Object key) {

    return mapLike.remove(key);
  }

  @Override
  public void putAll (Map<? extends String, ?> m) {

    mapLike.putAll(m);
  }

  @Override
  public void clear () {

    mapLike.clear();
  }

  @Override
  public Set<String> keySet () {

    return mapLike.keySet();
  }

  @Override
  public Collection<Object> values () {

    return mapLike.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet () {

    return mapLike.entrySet();
  }
}
