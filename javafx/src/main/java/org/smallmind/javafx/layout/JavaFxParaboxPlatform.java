package org.smallmind.javafx.layout;

import org.smallmind.nutsnbolts.layout.Orientation;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.Perimeter;

public class JavaFxParaboxPlatform implements ParaboxPlatform {

  @Override
  public double getRelatedGap () {

    return 0;
  }

  @Override
  public double getUnrelatedGap () {

    return 0;
  }

  @Override
  public Perimeter getFramePerimeter () {

    return null;
  }

  @Override
  public Orientation getOrientation () {

    return null;
  }
}
