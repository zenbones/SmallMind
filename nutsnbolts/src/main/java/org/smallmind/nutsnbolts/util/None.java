package org.smallmind.nutsnbolts.util;

public class None implements Option {

  private static None NONE = new None();

  public static None none () {

    return NONE;
  }

  private None () {
  }

  public boolean isNone () {

    return true;
  }

  public Object get () {

    return null;
  }
}
