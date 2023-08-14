/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.bayeux.cometd.extension;

import org.cometd.bayeux.Promise;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.message.MessageGenerator;
import org.smallmind.bayeux.cometd.session.OumuamuaLocalSession;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class ExtensionNotifier {

  public static boolean incoming (OumuamuaServer oumuamuaServer, OumuamuaServerSession sender, MessageGenerator messageGenerator) {

    boolean processing = true;

    if (sender.isLocalSession()) {
      for (ClientSession.Extension sessionExtension : new IterableIterator<>(((OumuamuaLocalSession)sender.getLocalSession()).iterateExtensions())) {

        Promise.Completable<Boolean> promise;

        sessionExtension.incoming(sender.getLocalSession(), messageGenerator.generate(), promise = new Promise.Completable<>());
        if (!promise.join()) {
          processing = false;
          break;
        }
      }
    }

    if (processing) {
      for (BayeuxServer.Extension serverExtension : new IterableIterator<>(oumuamuaServer.iterateExtensions())) {

        Promise.Completable<Boolean> promise;

        serverExtension.incoming(sender, messageGenerator.generate(), promise = new Promise.Completable<>());
        if (!promise.join()) {
          processing = false;
          break;
        }
      }
    }

    if (processing) {
      for (ServerSession.Extension sessionExtension : new IterableIterator<>(sender.iterateExtensions())) {

        Promise.Completable<Boolean> promise;

        sessionExtension.incoming(sender, messageGenerator.generate(), promise = new Promise.Completable<>());
        if (!promise.join()) {
          processing = false;
          break;
        }
      }
    }

    return processing;
  }

  public static boolean outgoing (OumuamuaServer oumuamuaServer, OumuamuaServerSession sender, OumuamuaServerSession receiver, MessageGenerator messageGenerator) {

    boolean processing = true;

    for (BayeuxServer.Extension serverExtension : new IterableIterator<>(oumuamuaServer.iterateExtensions())) {

      Promise.Completable<Boolean> promise;

      serverExtension.outgoing(sender, receiver, messageGenerator.generate(), promise = new Promise.Completable<>());
      if (!promise.join()) {
        processing = false;
        break;
      }
    }

    if (processing) {
      for (ServerSession.Extension sessionExtension : new IterableIterator<>(sender.iterateExtensions())) {

        Promise.Completable<ServerMessage.Mutable> promise;

        sessionExtension.outgoing(sender, receiver, messageGenerator.generate(), promise = new Promise.Completable<>());
        if (promise.join() == null) {
          processing = false;
          break;
        }
      }
    }

    if (processing && sender.isLocalSession()) {
      for (ClientSession.Extension sessionExtension : new IterableIterator<>(((OumuamuaLocalSession)sender.getLocalSession()).iterateExtensions())) {

        Promise.Completable<Boolean> promise;

        sessionExtension.outgoing(sender.getLocalSession(), messageGenerator.generate(), promise = new Promise.Completable<>());
        if (!promise.join()) {
          processing = false;
          break;
        }
      }
    }

    return processing;
  }
}
