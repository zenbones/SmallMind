package org.smallmind.cometd.oumuamua.channel;

import org.cometd.bayeux.server.ServerChannel;

public class ChannelNotice {

  private ServerChannel channel;

  public boolean isOn () {

    return channel != null;
  }

  public ServerChannel getChannel () {

    return channel;
  }

  public void setChannel (ServerChannel channel) {

    this.channel = channel;
  }
}
