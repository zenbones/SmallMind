package org.smallmind.cometd.oumuamua;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;

public class OumuamuaServerChannel implements ServerChannel {

  public static final String WILD_EPILOG = "/*";
  public static final String DEEP_WILD_EPILOG = "/**";

  private final OumuamuaServer oumuamuaServer;
  private final HashMap<String, ServerSession> subscriptionMap = new HashMap<>();
  private final HashMap<String, Object> attributeMap = new HashMap<>();
  private final LinkedList<ServerChannelListener> listenerList = new LinkedList<>();
  private final LinkedList<Authorizer> authorizerList = new LinkedList<>();
  private final ChannelId channelId;
  private final String id;
  private final boolean meta;
  private final boolean service;
  private final boolean broadcast;
  private final boolean wild;
  private final boolean deepWild;
  private boolean persistent;
  private boolean broadcastToPublisher;
  private long lazyTimeout = -1;

  public OumuamuaServerChannel (OumuamuaServer oumuamuaServer, String id, Authorizer... authorizers) {

    this.oumuamuaServer = oumuamuaServer;
    this.id = id;

    channelId = new ChannelId(id);
    meta = RequestParser.isMetaChannel(id);
    service = RequestParser.isServiceChannel(id);
    broadcast = !(meta || service);
    wild = id.endsWith(WILD_EPILOG);
    deepWild = id.startsWith(DEEP_WILD_EPILOG);
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public ChannelId getChannelId () {

    return channelId;
  }

  @Override
  public boolean isMeta () {

    return meta;
  }

  @Override
  public boolean isService () {

    return service;
  }

  @Override
  public boolean isBroadcast () {

    return broadcast;
  }

  @Override
  public boolean isWild () {

    return wild;
  }

  @Override
  public boolean isDeepWild () {

    return deepWild;
  }

  @Override
  public boolean isLazy () {

    return lazyTimeout >= 0;
  }

  @Override
  public void setLazy (boolean lazy) {

    if (lazyTimeout < 0) {
      lazyTimeout = 0;
    }
  }

  @Override
  public long getLazyTimeout () {

    return lazyTimeout;
  }

  @Override
  public void setLazyTimeout (long lazyTimeout) {

    this.lazyTimeout = lazyTimeout;
  }

  @Override
  public boolean isPersistent () {

    return persistent;
  }

  @Override
  public void setPersistent (boolean persistent) {

    this.persistent = persistent;
  }

  @Override
  public boolean isBroadcastToPublisher () {

    return broadcastToPublisher;
  }

  @Override
  public void setBroadcastToPublisher (boolean broadcastToPublisher) {

    this.broadcastToPublisher = broadcastToPublisher;
  }

  @Override
  public List<Authorizer> getAuthorizers () {

    return authorizerList;
  }

  @Override
  public void addAuthorizer (Authorizer authorizer) {

    authorizerList.add(authorizer);
  }

  @Override
  public void removeAuthorizer (Authorizer authorizer) {

    authorizerList.remove(authorizer);
  }

  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }

  @Override
  public Set<ServerSession> getSubscribers () {

    return new HashSet<>(subscriptionMap.values());
  }

  @Override
  public boolean subscribe (ServerSession session) {

    subscriptionMap.put(session.getId(), session);

    return true;
  }

  @Override
  public boolean unsubscribe (ServerSession session) {

    ServerSession serverSession;

    if ((serverSession = subscriptionMap.remove(session.getId())) == null) {

      return false;
    } else {
      // TODO: if no non-weak listeners and no subscriptions and non persistent then remove channel.

      return true;
    }
  }

  @Override
  public void publish (Session from, ServerMessage.Mutable message, Promise<Boolean> promise) {

  }

  @Override
  public void publish (Session from, Object data, Promise<Boolean> promise) {

  }

  @Override
  public void addListener (ServerChannelListener listener) {

    listenerList.add(listener);
  }

  @Override
  public void removeListener (ServerChannelListener listener) {

    listenerList.remove(listener);
    // TODO: if no non-weak listeners and no subscriptions and non persistent then remove channel.
  }

  @Override
  public List<ServerChannelListener> getListeners () {

    return listenerList;
  }

  @Override
  public void remove () {

    oumuamuaServer.cascadeRemoveChannel(this);
  }
}
