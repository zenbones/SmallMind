package org.smallmind.phalanx.wire.mock;

public interface MockMessageListener {

  public abstract boolean match (MockMessageProperties properties);

  public abstract void handle (MockMessage message);
}
