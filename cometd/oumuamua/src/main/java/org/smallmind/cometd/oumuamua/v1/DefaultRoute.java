package org.smallmind.cometd.oumuamua.v1;

public class DefaultRoute implements Route {

  @Override
  public String getPath () {

    return null;
  }

  @Override
  public boolean isWild () {

    return false;
  }

  @Override
  public boolean isDeepWild () {

    return false;
  }

  @Override
  public boolean isMeta () {

    return false;
  }

  @Override
  public boolean isService () {

    return false;
  }
}
