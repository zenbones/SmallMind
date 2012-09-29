package org.smallmind.nutsnbolts.layout;

public abstract class ComponentParaboxElement<C> extends ParaboxElement<C> {

  public ComponentParaboxElement (C component, Spec spec) {

    this(component, spec.staticConstraint());
  }

  public ComponentParaboxElement (C component, ParaboxConstraint constraint) {

    super(component, constraint);
  }

  public abstract void applyLayout (Pair location, Pair Size);

  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.PLANE;
  }
}
