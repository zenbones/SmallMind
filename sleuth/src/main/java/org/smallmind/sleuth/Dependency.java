package org.smallmind.sleuth;

import java.util.HashSet;

public class Dependency<T> {

  private Dependency<T> parent;
  private HashSet<Dependency<T>> children = new HashSet<>();
  private T value;
  private String name;
  private boolean temporary;
  private boolean permanent;

  public Dependency (String name) {

    this.name = name;
  }

  public Dependency (String name, T value) {

    this(name);

    this.value = value;
  }

  public String getName () {

    return name;
  }

  public T getValue () {

    return value;
  }

  public void setValue (T value) {

    this.value = value;
  }

  public Dependency<T> getParent () {

    return parent;
  }

  public void setParent (Dependency<T> parent) {

    this.parent = parent;
  }

  public HashSet<Dependency<T>> getChildren () {

    return children;
  }

  public void addChild (Dependency<T> dependency) {

    children.add(dependency);
  }

  public boolean isTemporary () {

    return temporary;
  }

  public void setTemporary () {

    temporary = true;
  }

  public void unsetTemporary () {

    temporary = false;
  }

  public boolean isPermanent () {

    return permanent;
  }

  public void setPermanent () {

    permanent = true;
  }

  @Override
  public int hashCode () {

    return name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj != null) && (obj instanceof Dependency) && ((name == null) ? (((Dependency<?>)obj).getName() == null) : name.equals(((Dependency<?>)obj).getName()));
  }
}
