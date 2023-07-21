package org.smallmind.cometd.oumuamua.extension;

import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.OumuamuaServerMessage;

public class ExtensionNotifier {

  public static boolean incoming (OumuamuaServer oumuamuaServer, OumuamuaServerSession sender, OumuamuaServerMessage.Mutable message) {

    boolean processing = true;

    for (BayeuxServer.Extension serverExtension : oumuamuaServer.getExtensions()) {

      Promise.Completable<Boolean> promise;

      serverExtension.incoming(sender, message, promise = new Promise.Completable<>());
      if (!promise.join()) {
        processing = false;
        break;
      }
    }

    if (processing) {
      for (ServerSession.Extension sessionExtension : sender.getExtensions()) {

        Promise.Completable<Boolean> promise;

        sessionExtension.incoming(sender, message, promise = new Promise.Completable<>());
        if (!promise.join()) {
          processing = false;
          break;
        }
      }
    }

    return processing;
  }

  public static boolean outgoing (OumuamuaServer oumuamuaServer, OumuamuaServerSession sender, OumuamuaServerSession receiver, OumuamuaServerMessage.Mutable message) {

    boolean processing = true;

    for (BayeuxServer.Extension serverExtension : oumuamuaServer.getExtensions()) {

      Promise.Completable<Boolean> promise;

      serverExtension.outgoing(sender, receiver, message, promise = new Promise.Completable<>());
      if (!promise.join()) {
        processing = false;
        break;
      }
    }

    if (processing) {

      for (ServerSession.Extension sessionExtension : receiver.getExtensions()) {

        Promise.Completable<ServerMessage.Mutable> promise;

        sessionExtension.outgoing(sender, receiver, message, promise = new Promise.Completable<>());
        if (promise.join() == null) {
          processing = false;
          break;
        }
      }
    }

    return processing;
  }
}
