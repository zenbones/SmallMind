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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Bayeux session that carries a unique snowflake id, manages its own lifecycle state machine,
 * and multiplexes inbound deliveries between a long-poll deque and direct streaming depending on
 * the active transport.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class OumuamuaSession<V extends Value<V>> extends AbstractAttributed implements Session<V> {

  private final ReentrantLock longPollLock = new ReentrantLock();
  private final Condition notEmptyCondition = longPollLock.newCondition();
  private final ConcurrentLinkedDeque<Pair<Session<V>, Packet<V>>> longPollDeque = new ConcurrentLinkedDeque<>();
  private final ConcurrentLinkedQueue<Session.Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicReference<SessionState> stateRef = new AtomicReference<>(SessionState.INITIALIZED);
  private final AtomicReference<Connection<V>> connectionRef = new AtomicReference<>();
  private final AtomicInteger longPollQueueSize = new AtomicInteger(0);
  private final Consumer<Session<V>> onConnectedCallback;
  private final Consumer<Session<V>> onDisconnectedCallback;
  private final AtomicBoolean longPolling = new AtomicBoolean(false);
  private final Level overflowLogLevel;
  private final String sessionId = SnowflakeId.newInstance().generateHexEncoding();
  private final long maxIdleTimeoutMilliseconds;
  private final int maxLongPollQueueSize;
  private long lastContactTimestamp;

  /**
   * Creates a session associated with the given connection, inheriting the long-polling mode from
   * the transport.
   *
   * @param onConnectedCallback        invoked with this session when its state transitions to
   *                                   {@link SessionState#CONNECTED}
   * @param onDisconnectedCallback     invoked with this session when its state transitions to
   *                                   {@link SessionState#DISCONNECTED}
   * @param connection                 the transport connection backing this session
   * @param maxLongPollQueueSize       maximum number of packets that may wait in the long-poll
   *                                   deque before the oldest entry is dropped
   * @param maxIdleTimeoutMilliseconds time without contact after which the session is eligible for
   *                                   removal
   * @param overflowLogLevel           log level used when the long-poll queue overflows; use
   *                                   {@code null} to suppress overflow logging
   */
  public OumuamuaSession (Consumer<Session<V>> onConnectedCallback, Consumer<Session<V>> onDisconnectedCallback, Connection<V> connection, int maxLongPollQueueSize, long maxIdleTimeoutMilliseconds, Level overflowLogLevel) {

    this.onConnectedCallback = onConnectedCallback;
    this.onDisconnectedCallback = onDisconnectedCallback;
    this.maxLongPollQueueSize = maxLongPollQueueSize;
    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;
    this.overflowLogLevel = (overflowLogLevel == null) ? Level.OFF : overflowLogLevel;

    if (connection.getTransport().getProtocol().isLongPolling()) {
      longPolling.set(true);
    }

    connectionRef.set(connection);
    lastContactTimestamp = System.currentTimeMillis();
  }

  /**
   * Passes the packet through all session-scoped {@link Session.PacketListener}s; only response
   * and delivery packets are processed.
   *
   * @param sender the session originating the packet, or {@code null} for server-side packets
   * @param packet the packet to process; the matching listener method is selected by packet type
   * @return the (possibly transformed) packet, or {@code null} if a listener vetoed it
   */
  private Packet<V> onProcessing (Session<V> sender, Packet<V> packet) {

    if (PacketType.RESPONSE.equals(packet.getPacketType()) || PacketType.DELIVERY.equals(packet.getPacketType())) {
      for (Session.Listener<V> listener : listenerList) {
        if (Session.PacketListener.class.isAssignableFrom(listener.getClass())) {
          if (PacketType.DELIVERY.equals(packet.getPacketType())) {
            if ((packet = ((Session.PacketListener<V>)listener).onDelivery(sender, packet)) == null) {
              break;
            }
          } else if ((packet = ((Session.PacketListener<V>)listener).onResponse(sender, packet)) == null) {
            break;
          }
        }
      }
    }

    return packet;
  }

  /**
   * Appends a listener to the session's listener chain.
   *
   * @param listener the listener to register
   */
  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  /**
   * Removes a listener from the session's listener chain.
   *
   * @param listener the listener to remove; no-op if not present
   */
  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }

  /**
   * Returns the unique hex-encoded snowflake id assigned to this session at creation.
   *
   * @return the immutable session identifier; never {@code null}
   */
  @Override
  public String getId () {

    return sessionId;
  }

  /**
   * Returns the capacity of the long-poll delivery deque before oldest entries are dropped.
   *
   * @return the maximum number of packets that may be queued for long polling
   */
  @Override
  public int getMaxLongPollQueueSize () {

    return maxLongPollQueueSize;
  }

  /**
   * Atomically replaces the underlying connection so that subsequent deliveries use the new
   * transport; called when a client reconnects and the existing session is reused.
   *
   * @param connection the new connection to associate with this session
   */
  public void hijack (Connection<V> connection) {

    connectionRef.set(connection);
  }

  /**
   * Delegates the cleanup notification to the currently associated connection so that transport
   * resources (e.g., open HTTP responses) can be released.
   */
  public void onCleanup () {

    Connection<V> connection;

    if ((connection = connectionRef.get()) != null) {
      connection.onCleanup();
    }
  }

  /**
   * Indicates whether the session's transport communicates within the same JVM rather than over
   * a network connection.
   *
   * @return {@code true} if the transport is local
   */
  @Override
  public boolean isLocal () {

    return connectionRef.get().getTransport().isLocal();
  }

  /**
   * Indicates whether this session is currently operating in long-polling mode.
   *
   * @return {@code true} if packets are queued for long-poll retrieval
   */
  @Override
  public boolean isLongPolling () {

    return longPolling.get();
  }

  /**
   * Overrides the long-polling mode detected from the transport, allowing the ack extension or
   * other mechanism to force queued delivery.
   *
   * @param longPolling {@code true} to route deliveries through the long-poll deque
   */
  @Override
  public void setLongPolling (boolean longPolling) {

    this.longPolling.set(longPolling);
  }

  /**
   * Returns the current lifecycle state of the session.
   *
   * @return one of {@link SessionState#INITIALIZED}, {@link SessionState#HANDSHOOK},
   * {@link SessionState#CONNECTED}, or {@link SessionState#DISCONNECTED}
   */
  @Override
  public synchronized SessionState getState () {

    return stateRef.get();
  }

  /**
   * Advances the session state to {@link SessionState#HANDSHOOK} after a successful handshake.
   */
  @Override
  public synchronized void completeHandshake () {

    stateRef.set(SessionState.HANDSHOOK);
  }

  /**
   * Advances the session state to {@link SessionState#CONNECTED} and fires the connected callback.
   */
  @Override
  public synchronized void completeConnection () {

    stateRef.set(SessionState.CONNECTED);
    onConnectedCallback.accept(this);
  }

  /**
   * Advances the session state to {@link SessionState#DISCONNECTED} and fires the disconnected
   * callback.
   */
  @Override
  public synchronized void completeDisconnect () {

    stateRef.set(SessionState.DISCONNECTED);
    onDisconnectedCallback.accept(this);
  }

  /**
   * Returns the transport backing the currently active connection.
   *
   * @return the current {@link Transport}; never {@code null}
   */
  public Transport<V> getTransport () {

    return connectionRef.get().getTransport();
  }

  /**
   * Records the current time as the last-contact timestamp, resetting the idle timer; does nothing
   * if the session is already disconnected.
   */
  public synchronized void contact () {

    if (!SessionState.DISCONNECTED.equals(stateRef.get())) {
      lastContactTimestamp = System.currentTimeMillis();
    }
  }

  /**
   * Determines whether the session has been idle beyond its configured maximum.
   *
   * @param now the current epoch millisecond timestamp to compare against the last-contact time
   * @return {@code true} if the elapsed time since last contact exceeds the idle timeout
   */
  public synchronized boolean isRemovable (long now) {

    return (now - lastContactTimestamp) >= maxIdleTimeoutMilliseconds;
  }

  /**
   * Passes a response packet through the session-scoped listener chain.
   *
   * @param sender the session generating the response
   * @param packet the response packet to process
   * @return the (possibly transformed) packet, or {@code null} if a listener vetoed it
   */
  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    return onProcessing(sender, packet);
  }

  /**
   * Writes the packet immediately to the underlying connection without queuing.
   *
   * @param packet the packet to send over the current connection
   */
  @Override
  public void dispatch (Packet<V> packet) {

    connectionRef.get().deliver(packet);
  }

  /**
   * Retrieves the next pending long-poll packet, blocking until one is available or the timeout
   * elapses.  The packet is run through the listener chain before being returned.
   *
   * @param timeout maximum time to wait
   * @param unit    unit for {@code timeout}
   * @return the next packet from the deque, processed by session listeners, or {@code null} if the
   * timeout expires before a packet arrives
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  @Override
  public Packet<V> poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    long remainingNanoseconds = unit.toNanos(timeout);

    longPollLock.lock();

    try {
      Pair<Session<V>, Packet<V>> enqueuedPair;

      do {
        if ((enqueuedPair = longPollDeque.pollFirst()) == null) {
          if (remainingNanoseconds > 0) {
            remainingNanoseconds = notEmptyCondition.awaitNanos(remainingNanoseconds);
          }
        } else {
          longPollQueueSize.decrementAndGet();

          // No need to re-freeze these packets, as they were frozen upon entering this session, and will be seen only by this connection
          return onProcessing(enqueuedPair.first(), enqueuedPair.second());
        }
      } while (remainingNanoseconds > 0);

      return null;
    } finally {
      longPollLock.unlock();
    }
  }

  /**
   * Routes an inbound delivery packet to this session: streaming channels with non-long-polling
   * transports bypass the queue and write directly; all other packets are enqueued in the
   * long-poll deque, dropping the oldest entry and logging if the queue is full.
   * Silently ignores the packet if the session is not in the {@link SessionState#CONNECTED} state.
   *
   * @param fromChannel the channel the packet was delivered through; its streaming flag drives
   *                    the dispatch path
   * @param sender      the session that published the packet, or {@code null} for server-side
   *                    delivery
   * @param packet      the already-frozen delivery packet
   */
  @Override
  public void deliver (Channel<V> fromChannel, Session<V> sender, Packet<V> packet) {

    if (SessionState.CONNECTED.equals(stateRef.get())) {
      // ignore the ack extension (or other forced long polling), *if* the protocol does not require long polling
      if (fromChannel.isStreaming() && (!connectionRef.get().getTransport().getProtocol().isLongPolling())) {

        Packet<V> processedPacket;

        // No need to re-freeze these packets, as they were frozen upon entering this session, and will be seen only by this connection
        if ((processedPacket = onProcessing(sender, packet)) != null) {
          connectionRef.get().deliver(processedPacket);
        }
      } else if (longPolling.get()) {
        longPollLock.lock();

        try {
          if (longPollQueueSize.incrementAndGet() > maxLongPollQueueSize) {
            LoggerManager.getLogger(OumuamuaSession.class).log(overflowLogLevel, "Session(%s) overflowed the long poll queue", getId());

            if (longPollDeque.pollFirst() != null) {
              longPollQueueSize.decrementAndGet();
            }
          }

          longPollDeque.add(new Pair<>(sender, packet));
          notEmptyCondition.signal();
        } finally {
          longPollLock.unlock();
        }
      } else {

        Packet<V> processedPacket;

        // No need to re-freeze these packets, as they were frozen upon entering this session, and will be seen only by this connection
        if ((processedPacket = onProcessing(sender, packet)) != null) {
          connectionRef.get().deliver(processedPacket);
        }
      }
    }
  }
}
