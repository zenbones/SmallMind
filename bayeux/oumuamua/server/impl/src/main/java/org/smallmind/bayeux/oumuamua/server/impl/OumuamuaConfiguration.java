package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.DefaultSerDes;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JacksonCodec;

public class OumuamuaConfiguration {

  private Backbone backbone;
  private Codec<?> codec = new JacksonCodec(new DefaultSerDes());
  private SecurityPolicy securityPolicy;
  private Protocol[] protocols;
  private long channelTimeToLiveMinutes = 30;

  public Backbone getBackbone () {

    return backbone;
  }

  public void setBackbone (Backbone backbone) {

    this.backbone = backbone;
  }

  public Codec<?> getCodec () {

    return codec;
  }

  public void setCodec (Codec<?> codec) {

    this.codec = codec;
  }

  public SecurityPolicy getSecurityPolicy () {

    return securityPolicy;
  }

  public void setSecurityPolicy (SecurityPolicy securityPolicy) {

    this.securityPolicy = securityPolicy;
  }

  public Protocol[] getProtocols () {

    return protocols;
  }

  public void setProtocols (Protocol[] protocols) {

    this.protocols = protocols;
  }

  public long getChannelTimeToLiveMinutes () {

    return channelTimeToLiveMinutes;
  }

  public void setChannelTimeToLiveMinutes (long channelTimeToLiveMinutes) {

    this.channelTimeToLiveMinutes = channelTimeToLiveMinutes;
  }
}
