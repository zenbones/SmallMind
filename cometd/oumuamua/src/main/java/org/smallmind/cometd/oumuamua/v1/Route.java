package org.smallmind.cometd.oumuamua.v1;

public interface Route {

  String getPath ();

  boolean isWild ();

  boolean isDeepWild ();

  boolean isMeta ();

  boolean isService ();

  default boolean isDeliverable () {

    return !(isWild() || isDeepWild() || isMeta() || isService());
  }
}
