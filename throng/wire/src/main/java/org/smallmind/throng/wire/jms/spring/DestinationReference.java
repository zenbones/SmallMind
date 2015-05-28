package org.smallmind.throng.wire.jms.spring;

public abstract class DestinationReference extends ManagedObjectReference {

  private String selector;
  private boolean durable = false;

  public abstract DestinationType getDestinationType ();

  public String getSelector () {

    return selector;
  }

  public void setSelector (String selector) {

    this.selector = selector;
  }

  public boolean isDurable () {

    return durable;
  }

  public void setDurable (boolean durable) {

    this.durable = durable;
  }
}
