package org.smallmind.file.ephemeral;

import java.nio.file.attribute.GroupPrincipal;
import javax.security.auth.Subject;

public class EphemeralGroupPrincipal implements GroupPrincipal {

  private String name;

  public EphemeralGroupPrincipal (String name) {

    this.name = name;
  }

  @Override
  public String getName () {

    return null;
  }

  @Override
  public boolean implies (Subject subject) {

    return GroupPrincipal.super.implies(subject);
  }
}
