package org.smallmind.nutsnbolts.layout;

public abstract class Group<G extends Group> {

  private Bias bias;

  public Group (Bias bias) {

    this.bias = bias;
  }

  public Bias getBias () {

    return bias;
  }

  public abstract void doLayout (double x, double y, double width, double height);
}
