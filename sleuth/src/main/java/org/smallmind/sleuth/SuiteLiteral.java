package org.smallmind.sleuth;

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;

public class SuiteLiteral extends AnnotationLiteral<Suite> implements Suite {

  @Override
  public String name () {

    return "default";
  }

  @Override
  public String[] dependsOn () {

    return new String[0];
  }
}
