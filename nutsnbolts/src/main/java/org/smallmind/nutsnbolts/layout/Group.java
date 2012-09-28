package org.smallmind.nutsnbolts.layout;

public abstract class Group<G extends Group> {

  private Bias bias;

  protected Group (Bias bias) {

    this.bias = bias;
  }

  public Bias getBias () {

    return bias;
  }

  public abstract void doLayout (double position, double measurement);
}
