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
package org.smallmind.quorum.pool.complex;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.nutsnbolts.lang.FormattedTimeoutException;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Time-ordered, background-driven queue that fires {@link DeconstructionFuse} instances
 * when their ignition times elapse.
 * <p>
 * Fuses are stored in a {@link ConcurrentSkipListMap} keyed by an {@link IgnitionKey} that
 * sorts first by ignition time and then by fuse ordinal, guaranteeing a deterministic firing
 * order when multiple fuses expire at the same millisecond. A single daemon task scheduled on a
 * {@link ScheduledExecutorService} wakes every second, removes all entries whose ignition time
 * is in the past, and calls {@link DeconstructionFuse#ignite()} on each.
 * <p>
 * A per-queue {@link AtomicInteger} issues monotonically increasing ordinals so that no two
 * fuses registered with this queue share the same ordinal.
 */
public class DeconstructionQueue {

  private final ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse> fuseMap = new ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse>();
  private final AtomicInteger ordinal = new AtomicInteger(0);

  private ScheduledExecutorService ignitionExecutor;

  /**
   * Starts the background daemon task that polls for expired fuses once per second.
   */
  public void startup () {

    ignitionExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> {

      Thread thread = new Thread(runnable, "quorum-deconstruction-queue");

      thread.setDaemon(true);

      return thread;
    });
    ignitionExecutor.scheduleWithFixedDelay(this::igniteExpired, 1, 1, TimeUnit.SECONDS);
  }

  /**
   * Returns the next unique ordinal for a newly constructed {@link DeconstructionFuse}.
   * Ordinals are strictly increasing across all fuses registered with this queue.
   *
   * @return the next ordinal value
   */
  public int nextOrdinal () {

    return ordinal.incrementAndGet();
  }

  /**
   * Registers a fuse for ignition at the time recorded in {@link DeconstructionFuse#getIgnitionTime()}.
   *
   * @param deconstructionFuse the fuse to schedule; its ignition time must already be set
   */
  public void add (DeconstructionFuse deconstructionFuse) {

    fuseMap.put(new IgnitionKey(deconstructionFuse), deconstructionFuse);
  }

  /**
   * Removes a previously registered fuse, cancelling its scheduled ignition.
   * Safe to call when the fuse is not currently in the queue.
   *
   * @param deconstructionFuse the fuse to cancel
   */
  public void remove (DeconstructionFuse deconstructionFuse) {

    fuseMap.remove(new IgnitionKey(deconstructionFuse));
  }

  /**
   * Shuts the background executor down and waits up to five seconds for the in-flight ignition pass
   * to finish.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the
   *                              worker to finish
   * @throws TimeoutException     if the background worker does not terminate within five seconds
   */
  public void shutdown ()
    throws InterruptedException, TimeoutException {

    ignitionExecutor.shutdown();
    if (!ignitionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
      throw new FormattedTimeoutException("Unable to terminate the deconstruction queue in (%d, %s)", 5, TimeUnit.SECONDS.name());
    }
  }

  /**
   * Removes all fuse-map entries at or before the current time and calls
   * {@link DeconstructionFuse#ignite()} on each. Logs any exception thrown by a fuse's ignition
   * logic, and any unexpected failure of the sweep itself, so that a single bad fuse cannot cancel
   * future runs. Invoked once per second by the background {@link ScheduledExecutorService}.
   */
  private void igniteExpired () {

    try {

      Map.Entry<IgnitionKey, DeconstructionFuse> firstEntry;
      long now = System.currentTimeMillis();

      while (((firstEntry = fuseMap.firstEntry()) != null) && (firstEntry.getKey().getIgnitionTime() <= now)) {

        DeconstructionFuse firstFuse;

        if ((firstFuse = fuseMap.remove(firstEntry.getKey())) != null) {
          try {
            firstFuse.ignite();
          } catch (Exception exception) {
            LoggerManager.getLogger(DeconstructionQueue.class).error(exception);
          }
        }
      }
    } catch (Exception exception) {
      LoggerManager.getLogger(DeconstructionQueue.class).error(exception);
    }
  }

  /**
   * Composite sort key that orders fuses by ignition time, then by ordinal when two fuses
   * share the same ignition time. Used as the key in the {@link ConcurrentSkipListMap}.
   * <p>
   * Equality is based solely on ordinal because each fuse has a unique ordinal; two keys with
   * the same ordinal always represent the same fuse.
   */
  private static class IgnitionKey implements Comparable<IgnitionKey> {

    private final long ignitionTime;
    private final int ordinal;

    private IgnitionKey (DeconstructionFuse deconstructionFuse) {

      this(deconstructionFuse.getOrdinal(), deconstructionFuse.getIgnitionTime());
    }

    private IgnitionKey (int ordinal, long ignitionTime) {

      this.ordinal = ordinal;
      this.ignitionTime = ignitionTime;
    }

    /**
     * Returns the ordinal component of this key.
     *
     * @return the fuse ordinal
     */
    public int getOrdinal () {

      return ordinal;
    }

    /**
     * Returns the ignition time component of this key.
     *
     * @return the ignition timestamp in milliseconds
     */
    public long getIgnitionTime () {

      return ignitionTime;
    }

    /**
     * Orders by ignition time first, then by ordinal to break ties.
     *
     * @param key the key to compare against
     * @return a negative integer, zero, or a positive integer as this key is less than,
     * equal to, or greater than {@code key}
     */
    @Override
    public int compareTo (IgnitionKey key) {

      int comparison;

      if ((comparison = Long.compare(ignitionTime, key.getIgnitionTime())) == 0) {

        return ordinal - key.getOrdinal();
      }

      return comparison;
    }

    /**
     * Returns the ordinal as the hash code, consistent with {@link #equals(Object)}.
     *
     * @return the fuse ordinal
     */
    @Override
    public int hashCode () {

      return ordinal;
    }

    /**
     * Two keys are equal when they share the same ordinal, meaning they represent the
     * same fuse regardless of ignition time.
     *
     * @param obj the object to compare
     * @return {@code true} if {@code obj} is an {@code IgnitionKey} with the same ordinal
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof IgnitionKey) && (ordinal == ((IgnitionKey)obj).getOrdinal());
    }
  }
}
