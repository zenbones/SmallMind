package org.smallmind.phalanx.wire.jms.spring;

public abstract class ManagedObjectReference {

  private String name;
  private String path;

  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  public String getPath () {

    return path;
  }

  public void setPath (String path) {

    this.path = path;
  }
}
