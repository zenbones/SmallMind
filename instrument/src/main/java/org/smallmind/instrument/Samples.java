package org.smallmind.instrument;

public enum Samples {

  UNIFORM {
    @Override
    public Sample createSample () {

      return new UniformSample(SAMPLE_SIZE);
    }
  },

  BIASED {
    @Override
    public Sample createSample () {

      return new ExponentiallyDecayingSample(SAMPLE_SIZE, 0.015);
    }
  };

  private static final int SAMPLE_SIZE = 1028;

  public abstract Sample createSample ();
}
