package org.smallmind.claxon.meter;

public class GaugeBuilder implements MeterBuilder<Gauge> {

  @Override
  public Gauge build () {

    return new Gauge();
  }
}
