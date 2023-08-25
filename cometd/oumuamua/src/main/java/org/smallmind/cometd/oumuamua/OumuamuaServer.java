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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.Transport;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.channel.ChannelIterator;
import org.smallmind.cometd.oumuamua.channel.ChannelTree;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public class OumuamuaServer implements BayeuxServer {

  private final ChannelTree channelTree = new ChannelTree();
  private final Map<String, OumuamuaTransport> transportMap;
  private final List<String> allowedList;
  private int emptyChannelTimeToLiveMinutes = 30;
  private SecurityPolicy securityPolicy;

  public OumuamuaServer (Map<String, OumuamuaTransport> transportMap, String... allowed) {

    this.transportMap = transportMap;

    this.allowedList = (allowed == null) ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(allowed));
  }

  public int getEmptyChannelTimeToLiveMinutes () {

    return emptyChannelTimeToLiveMinutes;
  }

  public void setEmptyChannelTimeToLiveMinutes (int emptyChannelTimeToLiveMinutes) {

    this.emptyChannelTimeToLiveMinutes = emptyChannelTimeToLiveMinutes;
  }

  public void start (ServletConfig servletConfig)
    throws ServletException {

    for (OumuamuaTransport transport : transportMap.values()) {
      transport.init(this, servletConfig);
    }
  }

  public void stop () {

  }

  public String getProtocolVersion () {

    return "1.0";
  }

  @Override
  public Set<String> getKnownTransportNames () {

    return transportMap.keySet();
  }

  @Override
  public Transport getTransport (String transport) {

    return transportMap.get(transport);
  }

  @Override
  public List<String> getAllowedTransports () {

    return allowedList;
  }

  @Override
  public SecurityPolicy getSecurityPolicy () {

    return securityPolicy;
  }

  @Override
  public void setSecurityPolicy (SecurityPolicy securityPolicy) {

    this.securityPolicy = securityPolicy;
  }

  @Override
  public Object getOption (String qualifiedName) {

    for (OumuamuaTransport transport : transportMap.values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        return transport.getOption(qualifiedName.substring(transport.getOptionPrefix().length()));
      }
    }

    return null;
  }

  @Override
  public void setOption (String qualifiedName, Object value) {

    for (OumuamuaTransport transport : transportMap.values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        transport.setOption(qualifiedName.substring(transport.getOptionPrefix().length()), value);
      }
    }
  }

  @Override
  public Set<String> getOptionNames () {

    HashSet<String> nameSet = new HashSet<>();

    for (OumuamuaTransport transport : transportMap.values()) {
      for (String name : transport.getOptionNames()) {
        nameSet.add(transport.getOptionPrefix() + name);
      }
    }

    return nameSet;
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
  public ServerChannel getChannel (String s) {

    return null;
  }

  @Override
  public List<ServerChannel> getChannels () {

    return null;
  }

  public ServerChannel findChannel (String id) {

    ChannelTree channelBranch;

    return ((channelBranch = channelTree.find(0, ChannelIdCache.generate(id))) == null) ? null : channelBranch.getServerChannel();
  }

  @Override
  public MarkedReference<ServerChannel> createChannelIfAbsent (String id, ConfigurableServerChannel.Initializer... initializers) {

    MarkedReference<ChannelTree> branchRef = channelTree.add(this, 0, ChannelIdCache.generate(id));

    if (branchRef.isMarked() && (initializers != null)) {
      for (ConfigurableServerChannel.Initializer initializer : initializers) {
        initializer.configureChannel(branchRef.getReference().getServerChannel());
      }
    }

    return new MarkedReference<>(branchRef.getReference().getServerChannel(), branchRef.isMarked());
  }

  public void removeChannel (ServerChannel channel) {

    // TODO: remove
    ChannelTree channelTree;
  }

  public void publishToChannel (String id, OumuamuaPacket packet) {

    channelTree.publish(new ChannelIterator(id), packet, new HashSet<>());
  }

  public void cascadeRemoveChannel (ServerChannel channel) {

    String branchId = channel.getId();

    if (channel.isWild()) {
      branchId = branchId.substring(0, branchId.length() - ChannelId.WILD.length() + 1);
    } else if (channel.isDeepWild()) {
      branchId = branchId.substring(0, branchId.length() - ChannelId.DEEPWILD.length() + 1);
    }

    // TODO: remove
    // channelTree.remove(1, branchId.split("/", -1));
  }
}
