package org.smallmind.cometd.oumuamua.v1.server;

import org.smallmind.cometd.oumuamua.v1.Message;

public interface Server {

  interface SessionListener {

    void onConnected (Session session);

    void onDisconnected (Session session);
  }

  interface MessageListener {

    void onRequest (Message message);

    void onResponse (Message message);

    void onDelivery (Message message);
  }

  void setSecurityPolicy (SecurityPolicy securityPolicy);
}
