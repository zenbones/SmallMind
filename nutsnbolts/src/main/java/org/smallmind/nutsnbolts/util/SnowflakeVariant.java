package org.smallmind.nutsnbolts.util;

public enum SnowflakeVariant {

  ORIGINAL((byte)0x0), CUSTOM((byte)0x3);

  private final byte code;

  SnowflakeVariant (byte code) {

    this.code = code;
  }

  public byte getCode () {

    return code;
  }
}
