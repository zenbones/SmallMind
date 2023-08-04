package org.smallmind.cometd.oumuamua.channel;

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.Authorizer;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerChannel;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.MessageGenerator;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class AuthenticatorUtility {

  public static boolean canOperate (OumuamuaServer oumuamuaServer, OumuamuaServerSession serverSession, OumuamuaServerChannel serverChannel, ChannelId channelId, MessageGenerator messageGenerator, Authorizer.Operation operation) {

    boolean empty = true;
    boolean denied = false;
    boolean granted = false;

    if (serverChannel != null) {
      for (Authorizer authorizer : new IterableIterator<>(serverChannel.iterateAuthorizers())) {

        Promise.Completable<Authorizer.Result> promise;
        Authorizer.Result result;

        empty = false;
        authorizer.authorize(operation, serverChannel.getChannelId(), serverSession, messageGenerator.generate(), promise = new Promise.Completable<>());
        if ((result = promise.join()) != null) {
          if (result.isDenied()) {
            denied = true;
            break;
          } else if (result.isGranted()) {
            granted = true;
          }
        }
      }
    }

    if (!denied) {
      for (String wildChannel : channelId.getWildIds()) {
        if (denied) {
          break;
        } else {

          OumuamuaServerChannel wildServerChannel;

          if ((wildServerChannel = oumuamuaServer.findChannel(wildChannel)) != null) {
            for (Authorizer wildAuthorizer : new IterableIterator<>(wildServerChannel.iterateAuthorizers())) {

              Promise.Completable<Authorizer.Result> promise;
              Authorizer.Result result;

              empty = false;
              wildAuthorizer.authorize(operation, wildServerChannel.getChannelId(), serverSession, messageGenerator.generate(), promise = new Promise.Completable<>());
              if ((result = promise.join()) != null) {
                if (result.isDenied()) {
                  denied = true;
                  break;
                } else if (result.isGranted()) {
                  granted = true;
                }
              }
            }
          }
        }
      }
    }

    return empty || ((!denied) && granted);
  }
}
