package org.smallmind.bayeux.oumuamua.server.api;

public enum Transports {

  WEBSOCKET("websocket"), LONG_POLLING("long-polling");

  private final String name;

  Transports (String name) {

    this.name = name;
  }

  public String getName () {

    return name;
  }
}
