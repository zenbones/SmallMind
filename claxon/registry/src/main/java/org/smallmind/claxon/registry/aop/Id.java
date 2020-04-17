package org.smallmind.claxon.registry.aop;

public @interface Id {

  String domain () default "";

  String name ();
}
