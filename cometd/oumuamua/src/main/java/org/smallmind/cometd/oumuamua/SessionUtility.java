package org.smallmind.cometd.oumuamua;

import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;

public class SessionUtility {

  public static OumuamuaServerSession from (Session session) {

    return (OumuamuaServerSession)(LocalSession.class.isAssignableFrom(session.getClass()) ? ((LocalSession)session).getServerSession() : session);
  }
}
