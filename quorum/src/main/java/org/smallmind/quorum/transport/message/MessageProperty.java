package org.smallmind.quorum.transport.message;

public enum MessageProperty {

  SERVICE("service"), EXCEPTION("exception");

  private String key;

  private MessageProperty (String key) {

    this.key = MessageProperty.class.getPackage().getName() + '.' + key;
  }

  public String getKey () {

    return key;
  }
}
