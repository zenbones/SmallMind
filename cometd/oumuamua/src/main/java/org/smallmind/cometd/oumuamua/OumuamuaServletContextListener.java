package org.smallmind.cometd.oumuamua;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.cometd.bayeux.server.BayeuxServer;

public class OumuamuaServletContextListener implements ServletContextListener {

  private OumuamuaServer oumuamuaServer;

  public void setOumuamuaServer (OumuamuaServer oumuamuaServer) {

    this.oumuamuaServer = oumuamuaServer;
  }

  @Override
  public void contextInitialized (ServletContextEvent servletContextEvent) {

    servletContextEvent.getServletContext().setAttribute(BayeuxServer.ATTRIBUTE, oumuamuaServer);
  }

  @Override
  public void contextDestroyed (ServletContextEvent servletContextEvent) {

    oumuamuaServer.stop();
  }
}
