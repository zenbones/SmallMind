package org.smallmind.phalanx.wire;

public class ArgumentInfo {

  private Class<?> parameterType;
  private int index;

  public ArgumentInfo (int index, Class<?> parameterType) {

    this.index = index;
    this.parameterType = parameterType;
  }

  public int getIndex () {

    return index;
  }

  public Class<?> getParameterType () {

    return parameterType;
  }
}
