/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.transport.mock;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * In-memory publish/subscribe topic for the mock transport.  A background daemon thread
 * continuously drains the message queue and delivers each message to every registered
 * {@link MockMessageListener} whose {@link MockMessageListener#match} method returns {@code true}
 * for that message's properties.
 */
public class MockTopic {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final QueueWorker worker;
  private final ConcurrentLinkedQueue<MockMessage> messageQueue = new ConcurrentLinkedQueue<>();
  private final ArrayList<MockMessageListener> listenerList = new ArrayList<>();

  /**
   * Constructs the topic and starts the background daemon thread that dispatches messages to
   * matching listeners.
   */
  public MockTopic () {

    Thread thread = new Thread(worker = new QueueWorker());

    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Registers a listener to be evaluated against every message published to this topic.
   *
   * @param listener the listener to add
   */
  public void addListener (MockMessageListener listener) {

    synchronized (listenerList) {
      listenerList.add(listener);
    }
  }

  /**
   * Unregisters a previously added listener so it no longer receives messages from this topic.
   *
   * @param listener the listener to remove
   */
  public void removeListener (MockMessageListener listener) {

    synchronized (listenerList) {
      listenerList.remove(listener);
    }
  }

  /**
   * Publishes a message to the topic.  The background worker subsequently delivers it to every
   * registered listener whose {@link MockMessageListener#match} returns {@code true}.
   *
   * @param message the message to publish
   */
  public void send (MockMessage message) {

    messageQueue.add(message);
  }

  /**
   * Background daemon worker that drains the topic queue and delivers each message to every
   * matching {@link MockMessageListener}.
   */
  private class QueueWorker implements Runnable {

    /**
     * Signals the poll loop to exit on its next iteration.
     */
    public void close () {

      closed.set(true);
    }

    /**
     * Continuously drains the topic queue and delivers each message to every registered listener
     * for which {@link MockMessageListener#match} returns {@code true}.  Sleeps 100 ms when the
     * queue is empty.
     */
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
            Thread.sleep(100);
          } catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(MockQueue.class).error(interruptedException);
          }
        }
      }
    }
  }
}
