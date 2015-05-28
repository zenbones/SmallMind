package org.smallmind.throng.wire;

public enum WireProperty {

  CALLER_ID("callerId"), CONTENT_TYPE("contentType"), CLOCK("clock"), INSTANCE_ID("instanceId");
  private String key;

  private WireProperty (String key) {

    this.key = key;
  }

  public String getKey () {

    return key;
  }
}