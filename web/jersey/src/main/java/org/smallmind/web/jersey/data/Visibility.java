package org.smallmind.web.jersey.data;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public enum Visibility {

  IN, OUT, BOTH;

  public boolean matches (Direction direction) {

    switch (this) {
      case IN:
        return Direction.IN.equals(direction);
      case OUT:
        return Direction.OUT.equals(direction);
      case BOTH:
        return true;
      default:
        throw new UnknownSwitchCaseException(this.name());
    }
  }
}
