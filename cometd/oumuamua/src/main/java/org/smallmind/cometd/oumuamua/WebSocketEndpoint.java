package org.smallmind.cometd.oumuamua;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class WebSocketEndpoint extends Endpoint implements MessageHandler.Whole<String> {

  @Override
  public void onOpen (Session wsSession, EndpointConfig config) {

    wsSession.addMessageHandler(this);
  }

  @Override
  public void onMessage (String data) {

  }

  @Override
  public void onClose (Session wsSession, CloseReason closeReason) {

  }

  @Override
  public void onError (Session wsSession, Throwable failure) {

  }
}
