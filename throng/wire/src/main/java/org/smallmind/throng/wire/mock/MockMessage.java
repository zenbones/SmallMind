package org.smallmind.throng.wire.mock;

public class MockMessage {

  private MockMessageProperties properties = new MockMessageProperties();
  private byte[] bytes;

  public MockMessage (byte[] bytes) {

    this.bytes = bytes;
  }

  public MockMessageProperties getProperties () {

    return properties;
  }

  public byte[] getBytes () {

    return bytes;
  }
}
