package org.smallmind.persistence.query;

public class NullWhereOperand implements WhereOperand<Void, Void> {

  private static final NullWhereOperand INSTANCE = new NullWhereOperand();

  public static NullWhereOperand instance () {

    return INSTANCE;
  }

  @Override
  public Class<? extends Void> getTargetClass () {

    return Void.class;
  }

  @Override
  public String getTypeHint () {

    return null;
  }

  @Override
  public Void getValue () {

    return null;
  }
}
