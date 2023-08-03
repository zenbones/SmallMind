package org.smallmind.cometd.oumuamua.transport;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.context.OumuamuaServletContext;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;

public class LongPollingCarrier implements OumuamuaCarrier {

  private static final String[] ACTUAL_TRANSPORTS = new String[] {"long-polling"};
  private final OumuamuaServletContext context;
  private final LongPollingTransport longPollingTransport;
  private final AsyncContext asyncContext;
  private boolean connected;

  public LongPollingCarrier (LongPollingTransport longPollingTransport, AsyncContext asyncContext)
    throws IOException {

    this.longPollingTransport = longPollingTransport;
    this.asyncContext = asyncContext;

    context = new OumuamuaServletContext((HttpServletRequest)asyncContext.getRequest());
    asyncContext.getResponse().getOutputStream().setWriteListener();
  }

  @Override
  public String[] getActualTransports () {

    return ACTUAL_TRANSPORTS;
  }

  @Override
  public BayeuxContext getContext () {

    return context;
  }

  @Override
  public String getUserAgent () {

    return ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : longPollingTransport.getMaxInterval();

    asyncContext.setTimeout(Math.max(adjustedIdleTimeout, 0));
  }

  @Override
  public synchronized boolean isConnected () {

    return connected;
  }

  @Override
  public synchronized void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public synchronized void send (OumuamuaPacket... packets)
    throws Exception {
  }

  @Override
  public synchronized OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    return new OumuamuaPacket[0];
  }

  @Override
  public synchronized void close ()
    throws IOException {

  }
}
