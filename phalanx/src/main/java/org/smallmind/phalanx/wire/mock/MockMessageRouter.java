package org.smallmind.phalanx.wire.mock;

public class MockMessageRouter {

  private final MockQueue talkRequestQueue = new MockQueue();
  private final MockTopic whisperRequestTopic = new MockTopic();
  private final MockTopic responseTopic = new MockTopic();

  public MockQueue getTalkRequestQueue () {

    return talkRequestQueue;
  }

  public MockTopic getWhisperRequestTopic () {

    return whisperRequestTopic;
  }

  public MockTopic getResponseTopic () {

    return responseTopic;
  }
}
