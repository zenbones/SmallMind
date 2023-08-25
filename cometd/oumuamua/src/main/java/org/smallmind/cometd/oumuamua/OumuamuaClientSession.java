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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;

public class OumuamuaClientSession implements ClientSession {

  private final ConcurrentHashMap<String, ClientSessionChannel> channelMap = new ConcurrentHashMap<>();

  @Override
  public String getId () {

    return null;
  }

  @Override
  public boolean isConnected () {

    return false;
  }

  @Override
  public boolean isHandshook () {

    return false;
  }

  @Override
  public void setAttribute (String name, Object value) {

  }

  @Override
  public Object getAttribute (String name) {

    return null;
  }

  @Override
  public Set<String> getAttributeNames () {

    return null;
  }

  @Override
  public Object removeAttribute (String name) {

    return null;
  }

  @Override
  public void addExtension (Extension extension) {

  }

  @Override
  public void removeExtension (Extension extension) {

  }

  @Override
  public List<Extension> getExtensions () {

    return null;
  }

  @Override
  public void handshake (Map<String, Object> template, MessageListener callback) {

  }

  @Override
  public void disconnect (MessageListener callback) {

  }

  @Override
  public ClientSessionChannel getChannel (String channelName) {

//    return channelMap;

    return null;
  }

  public void release (String channelName) {

    channelMap.remove(channelName);
  }

  @Override
  public void remoteCall (String target, Object data, MessageListener callback) {

  }

  @Override
  public void batch (Runnable batch) {

  }

  @Override
  public void startBatch () {

  }

  @Override
  public boolean endBatch () {

    return false;
  }
}
