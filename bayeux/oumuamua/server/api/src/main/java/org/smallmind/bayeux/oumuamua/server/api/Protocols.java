package org.smallmind.bayeux.oumuamua.server.api;

public enum Protocols {

  WEBSOCKET("websocket"), SERVLET("servlet");

  private final String name;

  Protocols (String name) {

    this.name = name;
  }

  public String getName () {

    return name;
  }
}
