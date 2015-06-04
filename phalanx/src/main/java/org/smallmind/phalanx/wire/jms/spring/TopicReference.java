package org.smallmind.phalanx.wire.jms.spring;

public class TopicReference extends DestinationReference {

  @Override
  public DestinationType getDestinationType () {

    return DestinationType.TOPIC;
  }
}
