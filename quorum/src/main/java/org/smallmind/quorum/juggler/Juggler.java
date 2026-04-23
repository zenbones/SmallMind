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
package org.smallmind.quorum.juggler;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Load-balancing pool that distributes resource requests randomly across a set of {@link JugglingPin}s,
 * blacklisting any pin that fails and optionally recovering it after a configurable delay.
 * <p>
 * Pins are drawn from two rotating lists — a source list and a target list — so that each call to
 * {@link #pickResource()} selects uniformly at random without replacement until the source list is
 * exhausted, at which point the roles of the two lists swap. This ensures every active pin is used
 * once per full rotation before any pin is repeated.
 * <p>
 * Failed pins are moved into a timestamp-keyed blacklist map. If {@code recoveryCheckSeconds} is
 * greater than zero, a daemon thread scans this map every three seconds and calls
 * {@link JugglingPin#recover()} on any entry whose timestamp has aged past the threshold; recovered
 * pins are silently returned to the target list.
 * <p>
 * All public methods are {@code synchronized} on the juggler instance. The blacklist map is a
 * {@link ConcurrentSkipListMap} so the recovery worker can read it without holding the juggler lock,
 * but the final promotion of a recovered pin is performed inside a {@code synchronized} block.
 *
 * @param <P> the type of provider used to construct the managed resources
 * @param <R> the type of resource served to callers
 */
public class Juggler<P, R> implements BlackList<R> {

  private final SecureRandom random = new SecureRandom();
  private final JugglingPinFactory<P, R> jugglingPinFactory;
  private final P[] providers;
  private final Class<P> providerClass;
  private final Class<R> resourceClass;
  private final int recoveryCheckSeconds;
  private ProviderRecoveryWorker recoveryWorker = null;
  private ArrayList<JugglingPin<R>> sourcePins;
  private ArrayList<JugglingPin<R>> targetPins;
  private ConcurrentSkipListMap<Long, BlacklistEntry<R>> blacklistMap;
  private ComponentStatus status = ComponentStatus.UNINITIALIZED;

  /**
   * Creates a juggler that replicates a single provider across {@code size} pins.
   * <p>
   * Equivalent to calling {@link #Juggler(Class, Class, int, JugglingPinFactory, Object[])} with
   * an array of {@code size} references all pointing to the same provider instance.
   *
   * @param providerClass        the runtime class of the provider, used for typed array creation
   * @param resourceClass        the runtime class of the resource, forwarded to the factory
   * @param recoveryCheckSeconds seconds between blacklist recovery scans; {@code 0} disables recovery
   * @param jugglingPinFactory   factory that constructs a pin from each provider
   * @param provider             provider instance to replicate across all slots
   * @param size                 number of pins (and therefore concurrent resource handles) to manage
   */
  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P provider, int size) {

    this(providerClass, resourceClass, recoveryCheckSeconds, jugglingPinFactory, generateArray(provider, providerClass, size));
  }

  /**
   * Creates a juggler backed by an explicit array of providers.
   * <p>
   * One pin is created per provider during {@link #initialize()}; duplicate provider references
   * are allowed and result in independently managed pins that happen to share the same provider.
   *
   * @param providerClass        the runtime class of the provider, used when building error messages
   * @param resourceClass        the runtime class of the resource, forwarded to the factory
   * @param recoveryCheckSeconds seconds between blacklist recovery scans; {@code 0} disables recovery
   * @param jugglingPinFactory   factory that constructs a pin from each provider
   * @param providers            provider instances to wrap, one pin per element
   */
  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P... providers) {

    this.providerClass = providerClass;
    this.resourceClass = resourceClass;
    this.recoveryCheckSeconds = recoveryCheckSeconds;
    this.jugglingPinFactory = jugglingPinFactory;
    this.providers = providers;
  }

  /**
   * Creates an array of {@code size} elements all holding the same provider reference.
   *
   * @param provider      the provider to fill into every element
   * @param providerClass the component type of the resulting array
   * @param size          the length of the resulting array
   * @param <P>           provider type
   * @return a new array of the requested length, every element set to {@code provider}
   */
  private static <P> P[] generateArray (P provider, Class<P> providerClass, int size) {

    P[] array = (P[])Array.newInstance(providerClass, size);

    Arrays.fill(array, provider);

    return array;
  }

  /**
   * Allocates the source and target pin lists, constructs a pin for each configured provider,
   * and shuffles the pins into a random initial order.
   * <p>
   * This method is idempotent: subsequent calls while the juggler is already initialized have
   * no effect. Must be called before {@link #startup()}.
   *
   * @throws JugglerResourceCreationException if {@link JugglingPinFactory#createJugglingPin} fails
   *                                          for any provider
   */
  public synchronized void initialize ()
    throws JugglerResourceCreationException {

    if (status.equals(ComponentStatus.UNINITIALIZED)) {
      sourcePins = new ArrayList<>(providers.length);
      targetPins = new ArrayList<>(providers.length);
      blacklistMap = new ConcurrentSkipListMap<>();

      for (P provider : providers) {
        targetPins.add(jugglingPinFactory.createJugglingPin(provider, resourceClass));
      }

      while (!targetPins.isEmpty()) {
        sourcePins.add(targetPins.remove(random.nextInt(targetPins.size())));
      }

      status = ComponentStatus.INITIALIZED;
    }
  }

  /**
   * Starts all pins without invoking an additional lifecycle hook on each resource.
   * Delegates to {@link #startup(Method, Object...) startup(null)}.
   */
  public synchronized void startup () {

    startup(null);
  }

  /**
   * Starts all pins, calling {@code method} on each resource if non-null.
   * <p>
   * Any pin that throws during start is immediately blacklisted and removed from active circulation.
   * If {@code recoveryCheckSeconds} is positive, a daemon recovery thread is started after all pins
   * have been processed.
   * <p>
   * This method is a no-op unless the juggler is in the {@code INITIALIZED} state.
   *
   * @param method lifecycle hook to invoke on each resource after starting, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  public synchronized void startup (Method method, Object... args) {

    if (status.equals(ComponentStatus.INITIALIZED)) {

      Thread recoveryThread;
      Iterator<JugglingPin<R>> sourcePinIter = sourcePins.iterator();

      while (sourcePinIter.hasNext()) {

        JugglingPin<R> pin = sourcePinIter.next();

        try {
          pin.start(method, args);
        } catch (JugglerResourceException jugglerResourceException) {
          try {
            LoggerManager.getLogger(Juggler.class).error(jugglerResourceException);
          } finally {
            sourcePinIter.remove();
            blacklistMap.put(System.currentTimeMillis(), new BlacklistEntry<>(pin, jugglerResourceException));
          }
        }
      }

      if (recoveryCheckSeconds > 0) {
        recoveryThread = new Thread(recoveryWorker = new ProviderRecoveryWorker(recoveryCheckSeconds));
        recoveryThread.setDaemon(true);
        recoveryThread.start();
      }

      status = ComponentStatus.STARTED;
    }
  }

  /**
   * Selects a resource at random from the active pin pool and returns it.
   * <p>
   * Pins are drawn from the source list uniformly at random. When the source list is empty the two
   * lists swap, giving every active pin an equal chance over each full rotation. Any pin that throws
   * from {@link JugglingPin#obtain()} is blacklisted on the spot and the method retries with the
   * remaining pins. When both lists are empty the accumulated blacklist causes are collected into a
   * single {@link NoAvailableJugglerResourceException} with all failures attached as suppressed
   * exceptions.
   *
   * @return a live resource obtained from the least-recently-used available pin
   * @throws NoAvailableJugglerResourceException if every pin has been blacklisted and no resource
   *                                             can be returned
   * @throws IllegalStateException               if the juggler is not in the initialized or started state
   */
  public synchronized R pickResource ()
    throws NoAvailableJugglerResourceException {

    if (!(status.equals(ComponentStatus.INITIALIZED) || status.equals(ComponentStatus.STARTED))) {
      throw new IllegalStateException("Juggler must be in the initialized or started state");
    }

    while (!(sourcePins.isEmpty() && targetPins.isEmpty())) {

      R resource;
      JugglingPin<R> pin;

      if (sourcePins.isEmpty()) {

        ArrayList<JugglingPin<R>> tempPins = sourcePins;

        sourcePins = targetPins;
        targetPins = tempPins;
      }

      pin = sourcePins.remove(random.nextInt(sourcePins.size()));
      try {
        resource = pin.obtain();
        targetPins.add(pin);

        return resource;
      } catch (Exception exception) {
        try {
          LoggerManager.getLogger(Juggler.class).error(exception);
        } finally {
          blacklistMap.put(System.currentTimeMillis(), new BlacklistEntry<>(pin, exception));
        }
      }
    }

    throw generateTerminatingException();
  }

  /**
   * Builds a {@link NoAvailableJugglerResourceException} whose primary cause is the most recent
   * blacklist entry and whose suppressed exceptions carry the remaining blacklist causes in
   * descending timestamp order.
   *
   * @return the aggregated exception representing all accumulated resource failures
   */
  private NoAvailableJugglerResourceException generateTerminatingException () {

    NoAvailableJugglerResourceException noAvailableJugglerResourceException = null;
    boolean first = true;

    for (BlacklistEntry<R> blacklistEntry : blacklistMap.descendingMap().values()) {
      if (first) {
        noAvailableJugglerResourceException = new NoAvailableJugglerResourceException(blacklistEntry.throwable(), "All available resources(%s) have been black listed", providerClass.getSimpleName());
      } else {
        noAvailableJugglerResourceException.addSuppressed(blacklistEntry.throwable());
      }
      first = false;
    }

    return noAvailableJugglerResourceException;
  }

  /**
   * Removes the pin identified by {@code blacklistEntry} from active circulation and records it in
   * the blacklist map, logging the event at INFO level.
   * <p>
   * If the pin is not found in either the source or target list — for example because it was already
   * blacklisted via a prior call — the method silently does nothing.
   *
   * @param blacklistEntry record holding the pin to quarantine and the exception that caused the failure
   */
  @Override
  public synchronized void addToBlackList (BlacklistEntry<R> blacklistEntry) {

    if (sourcePins.remove(blacklistEntry.jugglingPin())) {
      blacklistMap.put(System.currentTimeMillis(), blacklistEntry);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blacklistEntry.jugglingPin().describe());
    } else if (targetPins.remove(blacklistEntry.jugglingPin())) {
      blacklistMap.put(System.currentTimeMillis(), blacklistEntry);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blacklistEntry.jugglingPin().describe());
    }
  }

  /**
   * Stops all pins without invoking an additional lifecycle hook on each resource.
   * Delegates to {@link #shutdown(Method, Object...) shutdown(null)}.
   */
  public synchronized void shutdown () {

    shutdown(null);
  }

  /**
   * Stops all active pins, calling {@code method} on each resource if non-null.
   * <p>
   * The recovery worker, if running, is signalled to stop and awaited before any pins are touched.
   * Exceptions thrown during stop are logged and swallowed so that all pins receive the stop call.
   * Pins in the target list are drained into the source list first so that both lists are visited.
   * <p>
   * This method is a no-op unless the juggler is in the {@code STARTED} state.
   *
   * @param method lifecycle hook to invoke on each resource before stopping, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  public synchronized void shutdown (Method method, Object... args) {

    if (status.equals(ComponentStatus.STARTED)) {
      if (recoveryWorker != null) {
        try {
          recoveryWorker.abort();
        } catch (InterruptedException interruptedException) {
          LoggerManager.getLogger(Juggler.class).error(interruptedException);
        }
      }

      for (JugglingPin<R> pin : sourcePins) {
        try {
          pin.stop(method, args);
        } catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        }
      }
      while (!targetPins.isEmpty()) {

        JugglingPin<R> pin = targetPins.remove(0);

        try {
          pin.stop(method, args);
        } catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        } finally {
          sourcePins.add(pin);
        }
      }

      status = ComponentStatus.STOPPED;
    }
  }

  /**
   * Closes all pins and releases their resources without invoking an additional lifecycle hook.
   * Delegates to {@link #deconstruct(Method, Object...) deconstruct(null)}.
   */
  public synchronized void deconstruct () {

    deconstruct(null);
  }

  /**
   * Closes all pins, calling {@code method} on each resource if non-null before disposal.
   * <p>
   * Exceptions thrown during close are logged and swallowed. After this call the juggler
   * returns to the {@code UNINITIALIZED} state and must be re-initialized before use.
   * <p>
   * This method is a no-op unless the juggler is in the {@code STOPPED} state.
   *
   * @param method lifecycle hook to invoke on each resource before closing, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  public synchronized void deconstruct (Method method, Object... args) {

    if (status.equals(ComponentStatus.STOPPED)) {
      for (JugglingPin<R> pin : sourcePins) {
        try {
          pin.close(method, args);
        } catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        }
      }

      status = ComponentStatus.UNINITIALIZED;
    }
  }

  /**
   * Daemon thread that periodically scans the blacklist and attempts to recover failed pins.
   * <p>
   * The worker wakes every three seconds and inspects all blacklist entries whose recorded timestamp
   * is older than {@code recoveryCheckMillis}. For each such entry it calls
   * {@link JugglingPin#recover()}; if that returns {@code true} the pin is removed from the blacklist
   * and added to the juggler's target list inside a {@code synchronized} block. A fatal log message
   * is emitted if the pin cannot be found in the map at promotion time, which indicates a race that
   * should never occur.
   */
  private class ProviderRecoveryWorker implements Runnable {

    private final CountDownLatch terminationLatch;
    private final CountDownLatch exitLatch;
    private final long recoveryCheckMillis;

    /**
     * Creates a worker that checks for recoverable pins on the given interval.
     *
     * @param recoveryCheckSeconds interval in seconds between blacklist recovery scans
     */
    public ProviderRecoveryWorker (int recoveryCheckSeconds) {

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
      recoveryCheckMillis = recoveryCheckSeconds * 1000L;
    }

    /**
     * Signals the worker to stop and blocks until its thread has exited.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting for the
     *                              worker to finish
     */
    public void abort ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    /**
     * Main loop: waits up to three seconds on the termination latch, then scans the front of the
     * blacklist map for entries that have aged past {@code recoveryCheckMillis}. Each qualifying entry
     * is offered to {@link JugglingPin#recover()}; successful recoveries are promoted to the target
     * list under the juggler lock. The loop exits cleanly when the termination latch fires or if the
     * thread is interrupted.
     */
    @Override
    public void run () {

      try {
        while (!terminationLatch.await(3, TimeUnit.SECONDS)) {

          Map.Entry<Long, BlacklistEntry<R>> firstEntry;

          while (((firstEntry = blacklistMap.firstEntry()) != null) && ((firstEntry.getKey() + recoveryCheckMillis) <= System.currentTimeMillis())) {
            if (firstEntry.getValue().jugglingPin().recover()) {
              synchronized (Juggler.this) {

                JugglingPin<R> recoveredPin;

                if ((recoveredPin = blacklistMap.remove(firstEntry.getKey()).jugglingPin()) != null) {
                  targetPins.add(recoveredPin);
                  LoggerManager.getLogger(Juggler.class).warn("Recovered resource(%s) from black list", recoveredPin.describe());
                } else {
                  LoggerManager.getLogger(ProviderRecoveryWorker.class).fatal("We've lost a resource(%s), which should never occur - please notify a system administrator", providerClass.getSimpleName());
                }
              }
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ProviderRecoveryWorker.class).error(interruptedException);
      }

      exitLatch.countDown();
    }
  }
}
