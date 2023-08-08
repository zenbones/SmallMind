package org.smallmind.cometd.oumuamua.transport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class AbstractExpiringCarrier implements OumuamuaCarrier {

  private final IdleCheck idleCheck;
  private final long clientTimeoutMilliseconds;
  private final long idleCheckMilliseconds;
  private final long defaultMaxSessionIdleTimeout;
  private long lastContactMilliseconds;
  private long maxSessionIdleTimeout;

  public AbstractExpiringCarrier (long clientTimeoutMilliseconds, long idleCheckMilliseconds, long defaultMaxSessionIdleTimeout) {

    Thread idleCheckThread;

    this.clientTimeoutMilliseconds = clientTimeoutMilliseconds;
    this.idleCheckMilliseconds = idleCheckMilliseconds;
    this.defaultMaxSessionIdleTimeout = defaultMaxSessionIdleTimeout;

    maxSessionIdleTimeout = (clientTimeoutMilliseconds > 0) ? clientTimeoutMilliseconds : defaultMaxSessionIdleTimeout;

    idleCheckThread = new Thread(idleCheck = new IdleCheck());
    idleCheckThread.setDaemon(true);
    idleCheckThread.start();
  }

  public abstract void finishClosing ();

  public synchronized void updateLastContact () {

    lastContactMilliseconds = System.currentTimeMillis();
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : clientTimeoutMilliseconds;

    this.maxSessionIdleTimeout = (adjustedIdleTimeout >= 0) ? adjustedIdleTimeout : defaultMaxSessionIdleTimeout;
  }

  @Override
  public synchronized void close () {

    try {
      idleCheck.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(LocalCarrier.class).error(interruptedException);
    } finally {
      finishClosing();
    }
  }

  private class IdleCheck implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!finishLatch.await(idleCheckMilliseconds, TimeUnit.MILLISECONDS)) {
          if (lastContactMilliseconds > 0) {
            if (System.currentTimeMillis() > lastContactMilliseconds + maxSessionIdleTimeout) {
              finishLatch.countDown();
              finishClosing();
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
