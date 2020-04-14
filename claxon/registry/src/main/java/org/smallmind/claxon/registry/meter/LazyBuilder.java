package org.smallmind.claxon.registry.meter;

import org.smallmind.claxon.registry.Clock;

public class LazyBuilder<M extends Meter> implements MeterBuilder<M> {

  private MeterBuilder<M> builder;

  public LazyBuilder (MeterBuilder<M> builder) {

    this.builder = builder;
  }

  @Override
  public M build (Clock clock) {

    return builder.build(clock);
  }
}
