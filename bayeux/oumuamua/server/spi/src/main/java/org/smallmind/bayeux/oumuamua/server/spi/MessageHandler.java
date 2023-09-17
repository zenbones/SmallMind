package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

public class MessageHandler<V extends Value<V>> {

  public void foo (Protocol<V> protocol, Server<V> server, Connection<V> connection, Message<V>[] messages) {

    for (Message<V> message : messages) {

      String path = message.getChannel();
      String sessionId = message.getSessionId();
      Meta meta = Meta.from(path);
      Route route = Meta.PUBLISH.equals(meta) ? new DefaultRoute(path) : meta.getRoute();

      if (sessionId == null) {
        if (Meta.HANDSHAKE.equals(meta)) {

          // session creation callback

          Session<V> session = server.createSession(connection = transport.createConnection(server));
          connection.setSession(session);
          server.addSession(session);

          if (SessionState.DISCONNECTED.equals(session.getState())) {
            // error
          }
          connection.respond(protocol, server, session, message);
          // response callback
          if (SessionState.DISCONNECTED.equals(session.getState())) {
//disco callback
          }
        } else {
          // error
        }
      } else {

        Session<V> session;

        if ((session = server.getSession(sessionId)) == null) {
          // error
        } else if (!connection.validateSession()) {
          // error
        } else {
          if (SessionState.DISCONNECTED.equals(session.getState())) {
// error
          }
          connection.respond(protocol, server, session, message);
          // response callback
          if (SessionState.DISCONNECTED.equals(session.getState())) {
//disco callback
          }
        }
      }
    }
  }
}
