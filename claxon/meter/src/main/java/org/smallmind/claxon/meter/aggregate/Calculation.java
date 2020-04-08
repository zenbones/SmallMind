package org.smallmind.claxon.meter.aggregate;

public enum Calculation {

  PER_TIME {
    @Override
    public double execute (long accumulated, int n, long transpired, double nanosecondsInVelocity) {

      double timeFactor = nanosecondsInVelocity / transpired;

      return accumulated * timeFactor;
    }
  }, AVERAGE {
    @Override
    public double execute (long accumulated, int n, long transpired, double nanosecondsInVelocity) {

      return accumulated / ((double)n);
    }
  };

  public abstract double execute (long accumulated, int n, long transpired, double nanosecondsInVelocity);
}
