package org.smallmind.throng.wire;

import java.io.Serializable;

public abstract class Location implements Serializable {

  public abstract LocationType getType ();

  public abstract int getVersion ();

  public abstract String getService ();

  public abstract Function getFunction ();
}
