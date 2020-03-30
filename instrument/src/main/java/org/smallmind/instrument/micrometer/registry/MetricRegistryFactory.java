package org.smallmind.instrument.micrometer.registry;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ServiceLoader;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class MetricRegistryFactory {

  private static MetricRegistry METRIC_REGISTRY;

  static {

    Iterator<MetricRegistry> registryIter;

    registryIter = ServiceLoader.load(MetricRegistry.class, Thread.currentThread().getContextClassLoader()).iterator();
    if (!registryIter.hasNext()) {
      throw new StaticInitializationError("No provider found for MetricRegistry");
    }

    METRIC_REGISTRY = registryIter.next();

    if (registryIter.hasNext()) {

      LinkedList<String> implementationList = new LinkedList<>();

      while (registryIter.hasNext()) {
        implementationList.add(registryIter.next().getClass().getName());
      }

      String[] implementations = new String[implementationList.size()];
      implementationList.toArray(implementations);

      throw new StaticInitializationError("Found conflicting service implementations(%s) %s", MetricRegistry.class.getSimpleName(), Arrays.toString(implementations));
    }
  }

  public static MetricRegistry getMetricRegistry () {

    return METRIC_REGISTRY;
  }
}
