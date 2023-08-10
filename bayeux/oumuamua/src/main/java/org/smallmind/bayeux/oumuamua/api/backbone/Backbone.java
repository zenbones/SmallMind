package org.smallmind.bayeux.oumuamua.api.backbone;

import org.smallmind.bayeux.oumuamua.api.Message;

public interface Backbone {

  void publish (Message message);
}
