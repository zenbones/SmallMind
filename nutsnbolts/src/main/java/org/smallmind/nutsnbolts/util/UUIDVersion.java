package org.smallmind.nutsnbolts.util;

public enum UUIDVersion {

  DATE_TIME_MAC(1),
  DATE_TIME_MAC_DCE(2),
  NAMESPACE_MD5(3),
  RANDOM(4),
  NAMESPACE_SHA1(5),
  SNOWFLAKE_NATIVE(6),
  CUSTOM(7);

  private final int version;

  UUIDVersion (int version) {

    this.version = version;
  }

  public int getVersion () {

    return version;
  }
}
