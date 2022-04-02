package org.smallmind.file.ephemeral;

import java.nio.file.attribute.UserPrincipal;

public class EphemeralUserPrincipal implements UserPrincipal {

  private String name;

  public EphemeralUserPrincipal (String name) {

    this.name = name;
  }

  @Override
  public String getName () {

    return name;
  }
}
