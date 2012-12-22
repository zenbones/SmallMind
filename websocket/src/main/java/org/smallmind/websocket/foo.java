package org.smallmind.websocket;

import java.net.URI;

public class Foo {

  public static void main (String... args)
    throws Exception {

    new Websocket(URI.create("ws://devg2tc-1.aws.glu.com:8080/game-web-server/websocket"));
  }
}
