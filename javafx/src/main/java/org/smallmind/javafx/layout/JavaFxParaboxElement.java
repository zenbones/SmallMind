package org.smallmind.javafx.layout;

import javafx.scene.Node;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.ComponentParaboxElement;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Pair;

public class JavaFxParaboxElement extends ComponentParaboxElement<Node> {

  public JavaFxParaboxElement (Node node, Constraint constraint) {

    super(node, constraint);
  }

  @Override
  public double getComponentMinimumMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().minWidth(-1);
      case VERTICAL:
        return getPart().minHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getComponentPreferredMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().prefWidth(-1);
      case VERTICAL:
        return getPart().prefHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getComponentMaximumMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().maxWidth(-1);
      case VERTICAL:
        return getPart().maxHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getBaseline (Bias bias, double measurement) {

    return bias.equals(Bias.VERTICAL) ? getPart().getBaselineOffset() : getPart().prefHeight(-1);
  }

  @Override
  public void applyLayout (Pair location, Pair size) {

    getPart().resizeRelocate(location.getFirst(), location.getSecond(), size.getFirst(), size.getSecond());
  }
}
