package org.smallmind.claxon.registry.indigenous;

import org.smallmind.claxon.meter.Domain;
import org.smallmind.claxon.meter.Kind;
import org.smallmind.claxon.meter.MeterBuilder;
import org.smallmind.claxon.meter.Tag;

public enum IndigenousKind implements Kind<IndigenousKind> {

  SPEEDOMETER;

  @Override
  public MeterBuilder<?> build (Domain domain, Tag... tags) {

    return null;
  }
}
