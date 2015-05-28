package org.smallmind.throng.worker;

import java.util.concurrent.TransferQueue;
import org.smallmind.instrument.config.MetricConfiguration;

public interface WorkerFactory<W extends Worker<T>, T> {

  public W createWorker (MetricConfiguration metricConfiguration, TransferQueue<T> transferQueue);
}
