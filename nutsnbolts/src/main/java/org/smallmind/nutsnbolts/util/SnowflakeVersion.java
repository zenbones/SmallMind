package org.smallmind.nutsnbolts.util;

public enum SnowflakeVersion {

  TIME_MAC_ADDRESS((byte)0x0), RANDOM((byte)0x6), CUSTOM((byte)0x7);

  private final byte code;

  SnowflakeVersion (byte code) {

    this.code = code;
  }

  public byte getCode () {

    return code;
  }
}
