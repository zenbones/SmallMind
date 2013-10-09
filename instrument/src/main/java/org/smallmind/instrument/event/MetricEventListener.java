package org.smallmind.instrument.event;

import java.util.EventListener;

public interface MetricEventListener extends EventListener {

  public abstract void metricTriggered (MetricEvent metricEvent);
}
