package org.smallmind.phalanx.wire;

import java.util.HashMap;

public class UnknownContext extends WireContext {

  private HashMap<?, ?> stuff = new HashMap<>();

  public HashMap<?, ?> getStuff () {

    return stuff;
  }

  public void setStuff (HashMap<?, ?> stuff) {

    this.stuff = stuff;
  }
}