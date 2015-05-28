package org.smallmind.throng.worker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class Worker<T> implements Runnable, MetricConfigurationProvider {

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final MetricConfiguration metricConfiguration;
  private final TransferQueue<T> workTransferQueue;

  public Worker (MetricConfiguration metricConfiguration, TransferQueue<T> workTransferQueue) {

    this.metricConfiguration = metricConfiguration;
    this.workTransferQueue = workTransferQueue;
  }

  public abstract void engageWork (T transfer)
    throws Exception;

  public abstract void close ()
    throws Exception;

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  public void stop ()
    throws Exception {

    if (stopped.compareAndSet(false, true)) {
      close();
    }
    exitLatch.await();
  }

  @Override
  public void run () {

    long idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();

    try {
      while (!stopped.get()) {
        try {

          final T transfer;

          if ((transfer = workTransferQueue.poll(1, TimeUnit.SECONDS)) != null) {
            InstrumentationManager.instrumentWithChronometer(this, Clocks.EPOCH.getClock().getTimeNanoseconds() - idleStart, TimeUnit.NANOSECONDS, new MetricProperty("event", MetricType.WORKER_IDLE.getDisplay()));

            engageWork(transfer);
          }
        } catch (Exception exception) {
          LoggerManager.getLogger(this.getClass()).error(exception);
        } finally {
          idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();
        }
      }
    } finally {
      exitLatch.countDown();
    }
  }
}
