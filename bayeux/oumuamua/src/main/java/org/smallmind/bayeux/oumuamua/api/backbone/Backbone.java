package org.smallmind.bayeux.oumuamua.api.backbone;

import org.smallmind.bayeux.oumuamua.api.Message;
import org.smallmind.bayeux.oumuamua.api.server.Server;

public interface Backbone {

  void setServer (Server server);

  void publish (Message message);
}
