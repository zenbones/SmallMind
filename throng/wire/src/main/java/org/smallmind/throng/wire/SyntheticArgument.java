package org.smallmind.throng.wire;

public class SyntheticArgument {

  private Class<?> parameterType;
  private String name;

  public SyntheticArgument (String name, Class<?> parameterType) {

    this.name = name;
    this.parameterType = parameterType;
  }

  public String getName () {

    return name;
  }

  public Class<?> getParameterType () {

    return parameterType;
  }
}