package org.smallmind.cometd.oumuamua.v1.client;

import org.smallmind.cometd.oumuamua.v1.Message;

public interface Client {

  interface MessageListener {

    void onResponse (Message message);

    void onDelivery (Message message);
  }
}
