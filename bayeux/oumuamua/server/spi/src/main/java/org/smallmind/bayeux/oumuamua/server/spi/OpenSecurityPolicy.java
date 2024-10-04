package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public class OpenSecurityPolicy<V extends Value<V>> implements SecurityPolicy<V> {

  @Override
  public boolean canHandshake (Session<V> session, Message<V> message) {

    return true;
  }

  @Override
  public boolean canCreate (Session<V> session, String path, Message<V> message) {

    return true;
  }

  @Override
  public boolean canSubscribe (Session<V> session, Channel<V> channel, Message<V> message) {

    return true;
  }

  @Override
  public boolean canPublish (Session<V> session, Channel<V> channel, Message<V> message) {

    return true;
  }
}
