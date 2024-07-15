/*
 * Copyright (c) 2007 through 2024 David Berkman
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

  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P provider, int size) {

    this(providerClass, resourceClass, recoveryCheckSeconds, jugglingPinFactory, generateArray(provider, providerClass, size));
  }

  public Juggler (Class<P> providerClass, Class<R> resourceClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P... providers) {

    this.providerClass = providerClass;
    this.resourceClass = resourceClass;
    this.recoveryCheckSeconds = recoveryCheckSeconds;
    this.jugglingPinFactory = jugglingPinFactory;
    this.providers = providers;
  }

  private static <P> P[] generateArray (P provider, Class<P> providerClass, int size) {

    P[] array = (P[])Array.newInstance(providerClass, size);

    Arrays.fill(array, provider);

    return array;
  }

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

  public synchronized void startup () {

    startup(null);
  }

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

  private NoAvailableJugglerResourceException generateTerminatingException () {

    NoAvailableJugglerResourceException noAvailableJugglerResourceException = null;
    boolean first = true;

    for (BlacklistEntry<R> blacklistEntry : blacklistMap.descendingMap().values()) {
      if (first) {
        noAvailableJugglerResourceException = new NoAvailableJugglerResourceException(blacklistEntry.getThrowable(), "All available resources(%s) have been black listed", providerClass.getSimpleName());
      } else {
        noAvailableJugglerResourceException.addSuppressed(blacklistEntry.getThrowable());
      }
      first = false;
    }

    return noAvailableJugglerResourceException;
  }

  @Override
  public synchronized void addToBlackList (BlacklistEntry<R> blacklistEntry) {

    if (sourcePins.remove(blacklistEntry.getJugglingPin())) {
      blacklistMap.put(System.currentTimeMillis(), blacklistEntry);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blacklistEntry.getJugglingPin().describe());
    } else if (targetPins.remove(blacklistEntry.getJugglingPin())) {
      blacklistMap.put(System.currentTimeMillis(), blacklistEntry);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blacklistEntry.getJugglingPin().describe());
    }
  }

  public synchronized void shutdown () {

    shutdown(null);
  }

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

  public synchronized void deconstruct () {

    deconstruct(null);
  }

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

  private class ProviderRecoveryWorker implements Runnable {

    private final CountDownLatch terminationLatch;
    private final CountDownLatch exitLatch;
    private final long recoveryCheckMillis;

    public ProviderRecoveryWorker (int recoveryCheckSeconds) {

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
      recoveryCheckMillis = recoveryCheckSeconds * 1000L;
    }

    public void abort ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!terminationLatch.await(3, TimeUnit.SECONDS)) {

          Map.Entry<Long, BlacklistEntry<R>> firstEntry;

          while (((firstEntry = blacklistMap.firstEntry()) != null) && ((firstEntry.getKey() + recoveryCheckMillis) <= System.currentTimeMillis())) {
            if (firstEntry.getValue().getJugglingPin().recover()) {
              synchronized (Juggler.this) {

                JugglingPin<R> recoveredPin;

                if ((recoveredPin = blacklistMap.remove(firstEntry.getKey()).getJugglingPin()) != null) {
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
