package org.smallmind.throng.wire.jms.spring;

public class TopicReference extends DestinationReference {

  @Override
  public DestinationType getDestinationType () {

    return DestinationType.TOPIC;
  }
}
