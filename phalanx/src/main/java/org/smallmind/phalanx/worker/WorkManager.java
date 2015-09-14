package org.smallmind.phalanx.worker;

import java.lang.reflect.Array;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.scribe.pen.LoggerManager;

public class WorkManager<W extends Worker<T>, T> implements MetricConfigurationProvider {

  private final AtomicReference<State> stateRef = new AtomicReference<>(State.STOPPED);
  private final MetricConfiguration metricConfiguration;
  private final TransferQueue<T> transferQueue;
  private final Class<W> workerClass;
  private final int concurrencyLimit;
  private W[] workers;
  public WorkManager (MetricConfiguration metricConfiguration, Class<W> workerClass, int concurrencyLimit) {

    this.metricConfiguration = metricConfiguration;
    this.workerClass = workerClass;
    this.concurrencyLimit = concurrencyLimit;

    transferQueue = new LinkedTransferQueue<>();
  }

  public int getConcurrencyLimit () {

    return concurrencyLimit;
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  public void startUp (WorkerFactory<W, T> workerFactory)
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {

      workers = (W[])Array.newInstance(workerClass, concurrencyLimit);
      for (int index = 0; index < workers.length; index++) {

        Thread workerThread = new Thread(workers[index] = workerFactory.createWorker(metricConfiguration, transferQueue));

        workerThread.setDaemon(true);
        workerThread.start();
      }

      stateRef.set(State.STARTED);
    } else {
      while (State.STARTING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  public void execute (final T work)
    throws Exception {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new WorkManagerException("%s is not in the 'started' state", WorkManager.class.getSimpleName());
    }

    InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("event", MetricType.ACQUIRE_WORKER.getDisplay())) {

      @Override
      public void withChronometer ()
        throws InterruptedException {

        boolean success;

        do {
          success = transferQueue.tryTransfer(work, 1, TimeUnit.SECONDS);
        } while (!success);
      }
    });
  }

  public void shutDown ()
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {
      for (W worker : workers) {
        try {
          worker.stop();
        } catch (Exception exception) {
          LoggerManager.getLogger(WorkManager.class).error(exception);
        }
      }
      stateRef.set(State.STOPPED);
    } else {
      while (State.STOPPING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  private static enum State {STOPPED, STARTING, STARTED, STOPPING}
}

