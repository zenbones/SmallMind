package org.smallmind.claxon.registry;

import java.util.Objects;

public class Identifier {

  private String domain;
  private String name;

  public Identifier (String domain, String name) {

    this(name);

    this.domain = domain;
  }

  public Identifier (String name) {

    this.name = name;
  }

  public static Identifier instance (String domain, String name) {

    return new Identifier(domain, name);
  }

  public static Identifier instance (String name) {

    return new Identifier(name);
  }

  public String getDomain () {

    return domain;
  }

  public String getName () {

    return name;
  }

  @Override
  public int hashCode () {

    return ((domain == null) ? 0 : domain.hashCode() * 31) + name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Identifier) && Objects.equals(domain, ((Identifier)obj).getDomain()) && name.equals(((Identifier)obj).getName());
  }
}
