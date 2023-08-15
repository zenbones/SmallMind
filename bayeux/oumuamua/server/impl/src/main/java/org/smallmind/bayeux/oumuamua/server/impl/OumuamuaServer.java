package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Set;
import org.smallmind.bayeux.oumuamua.common.api.Message;
import org.smallmind.bayeux.oumuamua.common.api.MessageCodec;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.IllegalChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.IllegalPathException;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;

public class OumuamuaServer implements Server {

  @Override
  public Set<String> getAttributeNames () {

    return null;
  }

  @Override
  public Object getAttribute (String name) {

    return null;
  }

  @Override
  public void setAttribute (String name, Object value) {

  }

  @Override
  public Object removeAttribute (String name) {

    return null;
  }

  @Override
  public void addListener (Listener listener) {

  }

  @Override
  public void removeListener (Listener listener) {

  }

  @Override
  public Protocol getProtocol (String name) {

    return null;
  }

  @Override
  public Backbone getBackbone () {

    return null;
  }

  @Override
  public SecurityPolicy getSecurityPolicy () {

    return null;
  }

  @Override
  public MessageCodec<?> getMessageCodec () {

    return null;
  }

  @Override
  public Route getRoute (String path)
    throws IllegalPathException {

    return new DefaultRoute(path);
  }

  @Override
  public Channel findChannel (Route route) {

    return null;
  }

  @Override
  public Channel requireChannel (Route route, ChannelInitializer... initializers) {

    return null;
  }

  @Override
  public Channel removeChannel (Channel channel)
    throws IllegalChannelStateException {

    return null;
  }

  @Override
  public void deliver (Message<?> message) {

  }
}
