package org.smallmind.cometd.oumuamua.channel;

@FunctionalInterface
public interface ChannelOperation {

  void operate (ChannelTree channelTree);
}
