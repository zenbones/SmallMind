package org.smallmind.nutsnbolts.util;

public enum UUIDVariant {

  APOLLO_NETWORK_COMPUTING(0),
  LEACH_SALZ(1),
  MICROSOFT_COMPATABILITY(2),
  RESERVED(3);

  private final int variant;

  UUIDVariant (int variant) {

    this.variant = variant;
  }

  public int getVariant () {

    return variant;
  }
}
