package org.smallmind.phalanx.wire.transport.kafka;

public class TopicNames {

  // prefix = "wire"
  private final String prefix;

  public TopicNames (String prefix) {

    this.prefix = prefix;
  }

  public String getShoutTopicName (String serviceGroup) {

    return prefix + "-shout-" + serviceGroup;
  }

  public String getTalkTopicName (String serviceGroup) {

    return prefix + "-talk-" + serviceGroup;
  }

  public String getWhisperTopicName (String serviceGroup, String clusterId) {

    return prefix + "-whisper-" + serviceGroup + "-" + clusterId;
  }

  public String getResponseTopicName (String serviceGroup, String clusterId) {

    return prefix + "-response-" + serviceGroup + "-" + clusterId;
  }
}
