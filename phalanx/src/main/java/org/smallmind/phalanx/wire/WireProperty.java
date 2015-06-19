package org.smallmind.phalanx.wire;

public enum WireProperty {

  TRANSPORT_ID("transportId"), CONTENT_TYPE("contentType"), CLOCK("clock"), INSTANCE_ID("instanceId");
  private String key;

  private WireProperty (String key) {

    this.key = key;
  }

  public String getKey () {

    return key;
  }
}