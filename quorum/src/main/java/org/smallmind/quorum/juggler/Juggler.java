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
 * Coordinates a pool of {@link JugglingPin}s that each wrap a provider-specific resource. The class shuffles
 * access evenly across the available pins, blacklisting failed ones and optionally attempting recovery after
 * a configurable delay.
 *
 * @param <P> provider type used to create resources
 * @param <R> type of resource being served
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
   * Creates a juggler with a single provider replicated to the given size.
   *
   * @param providerClass        class of the provider instances
   * @param resourceClass        class of the resources produced by pins
   * @param recoveryCheckSeconds period in seconds to check for blacklisted resource recovery (0 disables recovery)
   * @param jugglingPinFactory   factory that constructs pins for providers
   * @param provider             provider instance to clone for each slot
   * @param size                 number of resources to manage
   */
  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P provider, int size) {

    this(providerClass, resourceClass, recoveryCheckSeconds, jugglingPinFactory, generateArray(provider, providerClass, size));
  }

  /**
   * Creates a juggler with explicit providers.
   *
   * @param providerClass        class of the provider instances
   * @param resourceClass        class of the resources produced by pins
   * @param recoveryCheckSeconds period in seconds to check for blacklisted resource recovery (0 disables recovery)
   * @param jugglingPinFactory   factory that constructs pins for providers
   * @param providers            provider instances to wrap
   */
  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P... providers) {

    this.providerClass = providerClass;
    this.resourceClass = resourceClass;
    this.recoveryCheckSeconds = recoveryCheckSeconds;
    this.jugglingPinFactory = jugglingPinFactory;
    this.providers = providers;
  }

  /**
   * Utility to create an array filled with the provided provider instance.
   *
   * @param provider      provider to repeat
   * @param providerClass provider class used for array creation
   * @param size          desired array length
   * @param <P>           provider type
   * @return populated provider array
   */
  private static <P> P[] generateArray (P provider, Class<P> providerClass, int size) {

    P[] array = (P[])Array.newInstance(providerClass, size);

    Arrays.fill(array, provider);

    return array;
  }

  /**
   * Initializes pin collections and constructs pins from the configured providers.
   *
   * @throws JugglerResourceCreationException if pin creation fails
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
   * Starts all pins without invoking any lifecycle hook.
   */
  public synchronized void startup () {

    startup(null);
  }

  /**
   * Starts all pins, optionally invoking a supplied lifecycle method with arguments on each resource.
   * Failed pins are blacklisted and removed from circulation.
   *
   * @param method lifecycle method to invoke on each resource prior to becoming available; may be {@code null}
   * @param args   arguments for the lifecycle method
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
   * Selects and obtains a resource from the available pins. Failed resources are blacklisted and suppressed
   * exceptions are accumulated before eventually throwing when no resources remain.
   *
   * @return an available resource
   * @throws NoAvailableJugglerResourceException if every resource is unavailable or blacklisted
   * @throws IllegalStateException               if the juggler has not been initialized or started
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
   * Builds a terminating exception that aggregates all suppressed blacklist causes.
   *
   * @return aggregated exception detailing resource failures
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
   * Adds the supplied entry to the blacklist, removing the pin from active circulation.
   *
   * @param blacklistEntry entry describing the failed pin and cause
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
   * Stops all pins without invoking any lifecycle hook.
   */
  public synchronized void shutdown () {

    shutdown(null);
  }

  /**
   * Stops all pins, optionally invoking a supplied lifecycle method with arguments. Recovery worker is aborted first.
   *
   * @param method lifecycle method to invoke on each pin
   * @param args   arguments for the lifecycle method invocation
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
   * Closes all pins without invoking any lifecycle hook.
   */
  public synchronized void deconstruct () {

    deconstruct(null);
  }

  /**
   * Closes all pins, optionally invoking a supplied lifecycle method with arguments.
   *
   * @param method lifecycle method to invoke during close
   * @param args   arguments for the lifecycle method
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
   * Worker that periodically attempts to recover blacklisted pins after the configured interval.
   */
  private class ProviderRecoveryWorker implements Runnable {

    private final CountDownLatch terminationLatch;
    private final CountDownLatch exitLatch;
    private final long recoveryCheckMillis;

    /**
     * Constructs a worker that will check for recovery every supplied number of seconds.
     *
     * @param recoveryCheckSeconds interval in seconds to wait between blacklist scans
     */
    public ProviderRecoveryWorker (int recoveryCheckSeconds) {

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
      recoveryCheckMillis = recoveryCheckSeconds * 1000L;
    }

    /**
     * Signals the worker to exit and waits for the thread to terminate.
     *
     * @throws InterruptedException if interrupted while waiting for termination
     */
    public void abort ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    /**
     * Periodically scans the blacklist and attempts to recover pins whose delay has expired.
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
