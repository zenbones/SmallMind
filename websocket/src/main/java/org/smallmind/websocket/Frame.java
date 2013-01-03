package org.smallmind.websocket;

import java.util.concurrent.ThreadLocalRandom;

public class Frame {

  public static byte[] pong () {

    byte[] out = new byte[6];
    byte[] mask = new byte[4];

    out[0] = (byte)0x8A;
    out[1] = (byte)0x80;

    ThreadLocalRandom.current().nextBytes(mask);
    System.arraycopy(mask, 0, out, 2, 4);

    return out;
  }
}
