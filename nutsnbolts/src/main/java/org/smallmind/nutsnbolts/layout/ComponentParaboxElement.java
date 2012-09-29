package org.smallmind.nutsnbolts.layout;

public abstract class ComponentParaboxElement<C> extends ParaboxElement<C> implements PlanarPart {

  public ComponentParaboxElement (C component, Spec spec) {

    this(component, spec.staticConstraint());
  }

  public ComponentParaboxElement (C component, ParaboxConstraint constraint) {

    super(component, constraint);

    for (Bias bias : Bias.values()) {

      double minimumMeasurement;
      double preferredMeasurement;
      double maximumMeasurement;

      if ((minimumMeasurement = getMinimumMeasurement(bias)) > (preferredMeasurement = getPreferredMeasurement(bias))) {
        throw new LayoutException("Layout component(%s) must yield min(%d)<=pref(%d) along the bias(%s)", component, minimumMeasurement, preferredMeasurement, bias.name());
      }
      if (preferredMeasurement > (maximumMeasurement = getMaximumMeasurement(bias))) {
        throw new LayoutException("Layout component(%s) must yield pref(%d)<=max(%d) along the bias(%s)", component, preferredMeasurement, maximumMeasurement, bias.name());
      }
    }
  }

  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.PLANE;
  }
}
