package org.smallmind.claxon.registry;

public class SimpleNamingStrategy implements NamingStrategy {

  @Override
  public String from (Class<?> caller) {

    return caller.getName();
  }
}
