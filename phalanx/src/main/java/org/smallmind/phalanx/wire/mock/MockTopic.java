package org.smallmind.phalanx.wire.mock;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class MockTopic {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final QueueWorker worker;
  private final ConcurrentLinkedQueue<MockMessage> messageQueue = new ConcurrentLinkedQueue<>();
  private final ArrayList<MockMessageListener> listenerList = new ArrayList<>();

  public MockTopic () {

    Thread thread = new Thread(worker = new QueueWorker());

    thread.setDaemon(true);
    thread.start();
  }

  public void addListener (MockMessageListener listener) {

    synchronized (listenerList) {
      listenerList.add(listener);
    }
  }

  public void removeListener (MockMessageListener listener) {

    synchronized (listenerList) {
      listenerList.remove(listener);
    }
  }

  public void send (MockMessage message) {

    messageQueue.add(message);
  }

  @Override
  protected void finalize () {

    worker.close();
  }

  private class QueueWorker implements Runnable {

    public void close () {

      closed.set(true);
    }

    @Override
    public void run () {

      while (!closed.get()) {

        MockMessage message;

        if ((message = messageQueue.poll()) != null) {
          synchronized (listenerList) {
            for (MockMessageListener listener : listenerList) {
              if (listener.match(message.getProperties())) {
                listener.handle(message);
              }
            }
          }
        } else {
          try {
            Thread.sleep(500);
          } catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(MockQueue.class).error(interruptedException);
          }
        }
      }
    }
  }
}