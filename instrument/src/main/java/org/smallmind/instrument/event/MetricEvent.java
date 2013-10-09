package org.smallmind.instrument.event;

import java.lang.reflect.Method;
import java.util.EventObject;
import org.smallmind.instrument.Metric;
import org.smallmind.instrument.context.MetricAddress;

public class MetricEvent extends EventObject {

  private MetricAddress metricAddress;
  private Method method;
  private Object[] args;

  public MetricEvent (Metric source, MetricAddress metricAddress, Method method, Object... args) {

    super(source);

    this.metricAddress = metricAddress;
    this.method = method;
    this.args = args;
  }

  public MetricAddress getMetricAddress () {

    return metricAddress;
  }

  public Method getMethod () {

    return method;
  }

  public Object[] getArgs () {

    return args;
  }
}
