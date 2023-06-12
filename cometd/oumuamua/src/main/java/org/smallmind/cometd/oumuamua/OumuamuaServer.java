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
import java.util.Set;
import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.Transport;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

public class OumuamuaServer implements BayeuxServer {

  @Override
  public Set<String> getKnownTransportNames () {

    return null;
  }

  @Override
  public Transport getTransport (String transport) {

    return null;
  }

  @Override
  public List<String> getAllowedTransports () {

    return null;
  }

  @Override
  public Object getOption (String qualifiedName) {

    return null;
  }

  @Override
  public void setOption (String qualifiedName, Object value) {

  }

  @Override
  public Set<String> getOptionNames () {

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
  public void addListener (BayeuxServerListener bayeuxServerListener) {

  }

  @Override
  public void removeListener (BayeuxServerListener bayeuxServerListener) {

  }

  @Override
  public ServerChannel getChannel (String s) {

    return null;
  }

  @Override
  public List<ServerChannel> getChannels () {

    return null;
  }

  @Override
  public MarkedReference<ServerChannel> createChannelIfAbsent (String s, ConfigurableServerChannel.Initializer... initializers) {

    return null;
  }

  @Override
  public ServerSession getSession (String s) {

    return null;
  }

  @Override
  public List<ServerSession> getSessions () {

    return null;
  }

  @Override
  public boolean removeSession (ServerSession serverSession) {

    return false;
  }

  @Override
  public LocalSession newLocalSession (String s) {

    return null;
  }

  @Override
  public ServerMessage.Mutable newMessage () {

    return null;
  }

  @Override
  public SecurityPolicy getSecurityPolicy () {

    return null;
  }

  @Override
  public void setSecurityPolicy (SecurityPolicy securityPolicy) {

  }
}
