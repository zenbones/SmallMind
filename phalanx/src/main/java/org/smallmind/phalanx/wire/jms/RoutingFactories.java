package org.smallmind.phalanx.wire.jms;

public class RoutingFactories {

  private final ManagedObjectFactory requestQueueFactory;
  private final ManagedObjectFactory requestTopicFactory;
  private final ManagedObjectFactory responseTopicFactory;

  public RoutingFactories (ManagedObjectFactory requestQueueFactory, ManagedObjectFactory requestTopicFactory, ManagedObjectFactory responseTopicFactory) {

    this.requestQueueFactory = requestQueueFactory;
    this.requestTopicFactory = requestTopicFactory;
    this.responseTopicFactory = responseTopicFactory;
  }

  public ManagedObjectFactory getRequestQueueFactory () {

    return requestQueueFactory;
  }

  public ManagedObjectFactory getRequestTopicFactory () {

    return requestTopicFactory;
  }

  public ManagedObjectFactory getResponseTopicFactory () {

    return responseTopicFactory;
  }
}
