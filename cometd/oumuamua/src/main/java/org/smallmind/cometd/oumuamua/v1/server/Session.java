package org.smallmind.cometd.oumuamua.v1.server;

import org.smallmind.cometd.oumuamua.v1.Message;

public interface Session {

  interface MessageListener {

    void onResponse (Message message);

    void onDelivery (Message message);
  }

  String getId ();

  boolean isHandshook ();

  boolean isConnected ();

  void deliver (Message message);
}
