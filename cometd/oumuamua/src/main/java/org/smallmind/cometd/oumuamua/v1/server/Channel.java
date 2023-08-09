package org.smallmind.cometd.oumuamua.v1.server;

import org.smallmind.cometd.oumuamua.v1.Message;
import org.smallmind.cometd.oumuamua.v1.Route;

public interface Channel {

  Route getRoute ();

  void deliver (Message message);

  interface MessageListener {

    void onDelivery(Message message);
  }
}
