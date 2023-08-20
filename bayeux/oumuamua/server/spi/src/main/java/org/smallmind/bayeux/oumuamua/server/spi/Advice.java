package org.smallmind.bayeux.oumuamua.server.spi;

public enum Advice {

  INTERVAL("interval"), RECONNECT("reconnect");

  private final String field;

  Advice (String field) {

    this.field = field;
  }

  public String getField () {

    return field;
  }
}
