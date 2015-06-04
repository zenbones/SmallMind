package org.smallmind.phalanx.wire;

public class ProtoWireContext extends WireContext {

  private Object guts;
  private String skin;

  public ProtoWireContext (String skin, Object guts) {

    this.skin = skin;
    this.guts = guts;
  }

  public Object getGuts () {

    return guts;
  }

  public String getSkin () {

    return skin;
  }
}