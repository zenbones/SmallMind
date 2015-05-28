package org.smallmind.throng.wire.jms.spring;

public class QueueReference extends DestinationReference {

  @Override
  public DestinationType getDestinationType () {

    return DestinationType.TOPIC;
  }
}
