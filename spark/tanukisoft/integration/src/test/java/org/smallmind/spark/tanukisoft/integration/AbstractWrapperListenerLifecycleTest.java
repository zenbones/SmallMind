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
package org.smallmind.spark.tanukisoft.integration;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.property.PropertyExpanderException;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.spring.PropertyPlaceholderStringValueResolver;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorContextManager;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.tanukisoftware.wrapper.WrapperManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

// Exercises the wrapper-coupled surfaces (start, controlEvent, the main bootstrap) against the shadowed WrapperManager
// double declared under src/test/java/org/tanukisoftware/wrapper. Listeners that need the keep-alive to fire more
// than once override getStartupTimeoutSeconds() with a small value so the signaller runs quickly and the start()
// executor closes promptly; the property path is exercised with values the real wrapper would accept.
@org.testng.annotations.Test(groups = "unit")
public class AbstractWrapperListenerLifecycleTest {

  private static void assertUnbound () {

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive the wrapper callback");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  @BeforeMethod
  public void resetWrapperManager () {

    WrapperManager.reset();
  }

  public void testStartForwardsArgumentsAndReportsSuccess () {

    RecordingListener listener = new RecordingListener(false);
    Integer result = listener.start(new String[] {"alpha", "beta"});

    Assert.assertNull(result);
    Assert.assertEquals(listener.startupArgs, new String[] {"alpha", "beta"});
  }

  // The keep-alive signals the wrapper as soon as start() is entered, promising the configured timeout as the
  // millisecond wait hint the wrapper reads, rather than waiting for most of that timeout to elapse first.
  public void testStartSignalsTheWrapperImmediatelyWithTheConfiguredTimeoutInMilliseconds () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "10");

    RecordingListener listener = new RecordingListener(false, 1, null);

    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertTrue(WrapperManager.signalStartingCount() >= 1);
    Assert.assertEquals(WrapperManager.lastSignalStartingTimeout(), 10000);
  }

  // With no wrapper.startup.timeout property the wrapper's own default of 30 seconds is promised.
  public void testStartSignalsTheDefaultTimeoutWhenNoneIsConfigured () {

    Assert.assertNull(new RecordingListener(false, 1, null).start(new String[] {"alpha"}));
    Assert.assertEquals(WrapperManager.lastSignalStartingTimeout(), 30000);
  }

  public void testStartHonoursAnExplicitStartupTimeout () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "45");
    Assert.assertNull(new RecordingListener(false, 1, null).start(new String[] {"alpha"}));
    Assert.assertEquals(WrapperManager.lastSignalStartingTimeout(), 45000);
  }

  // A keep-alive signal that throws is printed but does not end the schedule; the following firing still happens.
  public void testStartKeepsSignallingAfterASignalThrows () {

    RecordingListener listener = new RecordingListener(false, 2, 2);

    WrapperManager.setSignalStartingThrows(true);
    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertTrue(WrapperManager.signalStartingCount() >= 2);
  }

  public void testStartTranslatesStartupFailureIntoTheStackTraceCode () {

    Assert.assertEquals(new RecordingListener(true).start(new String[] {"alpha"}), Integer.valueOf(2));
  }

  // Errors (NoClassDefFoundError and friends are the usual startup failures) get the same exit code as exceptions.
  public void testStartTranslatesAStartupErrorIntoTheStackTraceCode () {

    Assert.assertEquals(new ErrorThrowingListener().start(new String[] {"alpha"}), Integer.valueOf(2));
  }

  // A misconfigured wrapper.startup.timeout is reported from start(), the first point at which the wrapper
  // configuration is visible, and startup() is never reached.
  public void testStartRejectsAStartupTimeoutBelowTheMinimum () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "5");

    RecordingListener listener = new RecordingListener(false);

    Assert.assertEquals(listener.start(new String[] {"alpha"}), Integer.valueOf(2));
    Assert.assertNull(listener.startupArgs);
    Assert.assertEquals(WrapperManager.signalStartingCount(), 0);
  }

  public void testStartRejectsAnUnparsableStartupTimeout () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "soon");

    RecordingListener listener = new RecordingListener(false);

    Assert.assertEquals(listener.start(new String[] {"alpha"}), Integer.valueOf(2));
    Assert.assertNull(listener.startupArgs);
  }

  // Zero is the wrapper's own "wait indefinitely" setting, so there is nothing to keep alive.
  public void testStartSkipsTheKeepAliveWhenTheWrapperWaitsIndefinitely () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "0");

    RecordingListener listener = new RecordingListener(false);

    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertEquals(listener.startupArgs, new String[] {"alpha"});
    Assert.assertEquals(WrapperManager.signalStartingCount(), 0);
  }

  // With a one-second ceiling and a one-second cadence only the immediate firing can signal; the next firing lands
  // past the ceiling and cancels the schedule instead.
  public void testKeepAliveStopsSignallingAtTheFailSafeCeiling () {

    Assert.assertNull(new HoldingListener(1, 2600).start(new String[] {"alpha"}));
    Assert.assertTrue(WrapperManager.signalStartingCount() <= 1, "signals: " + WrapperManager.signalStartingCount());
  }

  // A non-positive ceiling removes the stop, so the same hold keeps signalling at every firing.
  public void testKeepAliveRunsOnWithoutAFailSafeCeiling () {

    Assert.assertNull(new HoldingListener(0, 2600).start(new String[] {"alpha"}));
    Assert.assertTrue(WrapperManager.signalStartingCount() >= 3, "signals: " + WrapperManager.signalStartingCount());
  }

  // Without an override the ceiling comes from spark.startup.ceiling in the wrapper configuration.
  public void testKeepAliveReadsTheCeilingFromTheWrapperConfiguration () {

    WrapperManager.getProperties().setProperty("spark.startup.ceiling", "1");
    Assert.assertNull(new HoldingListener(null, 2600).start(new String[] {"alpha"}));
    Assert.assertTrue(WrapperManager.signalStartingCount() <= 1, "signals: " + WrapperManager.signalStartingCount());
  }

  public void testStartTranslatesAnUnparsableStartupCeilingIntoTheStackTraceCode () {

    WrapperManager.getProperties().setProperty("spark.startup.ceiling", "later");
    Assert.assertEquals(new HoldingListener(null, 0).start(new String[] {"alpha"}), Integer.valueOf(2));
  }

  public void testControlEventStopsTheApplicationWhenNotNativelyControlled () {

    WrapperManager.setControlledByNativeWrapper(false);
    new RecordingListener(false).controlEvent(WrapperManager.WRAPPER_CTRL_C_EVENT);
    Assert.assertEquals(WrapperManager.lastStopCode(), Integer.valueOf(0));
  }

  public void testControlEventHonoursCloseAndShutdownEvents () {

    WrapperManager.setControlledByNativeWrapper(false);
    new RecordingListener(false).controlEvent(WrapperManager.WRAPPER_CTRL_CLOSE_EVENT);
    Assert.assertEquals(WrapperManager.lastStopCode(), Integer.valueOf(0));

    WrapperManager.reset();
    WrapperManager.setControlledByNativeWrapper(false);
    new RecordingListener(false).controlEvent(WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT);
    Assert.assertEquals(WrapperManager.lastStopCode(), Integer.valueOf(0));
  }

  public void testControlEventIgnoresUnrelatedEvents () {

    WrapperManager.setControlledByNativeWrapper(false);
    new RecordingListener(false).controlEvent(WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT);
    Assert.assertNull(WrapperManager.lastStopCode());
  }

  public void testControlEventDefersToTheNativeWrapperWhenControlled () {

    WrapperManager.setControlledByNativeWrapper(true);
    new RecordingListener(false).controlEvent(WrapperManager.WRAPPER_CTRL_C_EVENT);
    Assert.assertNull(WrapperManager.lastStopCode());
  }

  // The listener extends PerApplicationContext and runs startup() inside its scope, so a bridging manager called from
  // startup() writes both maps; a thread the application starts without the scope still finds the registration
  // through the context it inherits, and a thread handed the scope finds it there.
  public void testStartupRegistrationsReachBothBindings ()
    throws Exception {

    BridgeProbingListener listener = new BridgeProbingListener();

    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertSame(listener.seenOnUnwrappedThread, listener.accessor, "an un-wrapped child thread must resolve through the inherited context");
    Assert.assertSame(listener.getPerApplicationScope().call(SpringPropertyAccessorManager::getSpringPropertyAccessor), listener.accessor, "the scope must hold the same registration");
    Assert.assertSame(SpringPropertyAccessorContextManager.getSpringPropertyAccessor(), listener.accessor, "the context must hold the same registration");
  }

  // startup() runs inside the listener's own scope: data it registers is visible through that scope afterwards,
  // and the calling thread is unbound once start() returns.
  public void testStartRunsStartupInsideTheListenersScope () {

    ScopeProbingListener listener = new ScopeProbingListener();

    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertEquals(listener.registered(), "from-startup");
    assertUnbound();
  }

  public void testStopRunsShutdownInsideTheListenersScope () {

    ScopeProbingListener listener = new ScopeProbingListener();

    Assert.assertNull(listener.start(new String[] {"alpha"}));
    Assert.assertEquals(listener.stop(0), 0);
    Assert.assertEquals(listener.seenOnShutdown, "from-startup");
    assertUnbound();
  }

  // The wrapper's stop() is reached from inside controlEvent's binding, so the double's stop hook sees the scope.
  public void testControlEventRunsInsideTheListenersScope () {

    ScopeProbingListener listener = new ScopeProbingListener();
    AtomicReference<String> seenOnStop = new AtomicReference<>();

    WrapperManager.setControlledByNativeWrapper(false);
    WrapperManager.setStopHook(() -> seenOnStop.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));
    Assert.assertNull(listener.start(new String[] {"alpha"}));
    listener.controlEvent(WrapperManager.WRAPPER_CTRL_C_EVENT);
    Assert.assertEquals(WrapperManager.lastStopCode(), Integer.valueOf(0));
    Assert.assertEquals(seenOnStop.get(), "from-startup");
    assertUnbound();
  }

  // The listener is instantiated by name and handed to the wrapper along with the (expanded) arguments, unchanged.
  public void testMainBootstrapsTheListener ()
    throws Throwable {

    AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha");
    Assert.assertTrue(WrapperManager.startedListener() instanceof CapturingWrapperListener);
    Assert.assertEquals(WrapperManager.startedArguments(), new String[] {"alpha"});
  }

  // The wrapper configuration is not consulted by main(), so nothing from it leaks into the arguments; start()
  // reads it once the JVM is connected.
  public void testMainLeavesTheWrapperConfigurationToStart ()
    throws Throwable {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "soon");
    WrapperManager.getProperties().setProperty("spark.startup.ceiling", "later");
    AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha");
    Assert.assertEquals(WrapperManager.startedArguments(), new String[] {"alpha"});
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  // Registers a value during startup and reads it back from inside shutdown and controlEvent, proving all three run
  // bound to the same scope instance.
  private static class ScopeProbingListener extends AbstractWrapperListener {

    private String seenOnShutdown;

    private String registered () {

      return getPerApplicationScope().call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
    }

    @Override
    protected int getStartupTimeoutSeconds () {

      return 0;
    }

    @Override
    public void startup (String[] args) {

      PerApplicationScope.setPerApplicationData(SampleManager.class, "from-startup");
    }

    @Override
    public void shutdown () {

      seenOnShutdown = PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
    }
  }

  private static SpringPropertyAccessor emptyAccessor () {

    try {

      return new SpringPropertyAccessor(new PropertyPlaceholderStringValueResolver(new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false), new HashMap<>()));
    } catch (PropertyExpanderException propertyExpanderException) {
      throw new RuntimeException(propertyExpanderException);
    }
  }

  // Registers through a bridging manager during startup and, still inside startup, starts a plain thread that reads
  // it back through the same manager without being handed the scope.
  private static class BridgeProbingListener extends AbstractWrapperListener {

    private final SpringPropertyAccessor accessor = emptyAccessor();
    private SpringPropertyAccessor seenOnUnwrappedThread;

    @Override
    protected int getStartupTimeoutSeconds () {

      return 0;
    }

    @Override
    public void startup (String[] args) {

      SpringPropertyAccessorManager.register(accessor);

      Thread thread = new Thread(() -> seenOnUnwrappedThread = SpringPropertyAccessorManager.getSpringPropertyAccessor());

      thread.start();
      try {
        thread.join();
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void shutdown () {

    }
  }

  private static class ErrorThrowingListener extends AbstractWrapperListener {

    @Override
    public void startup (String[] args) {

      throw new NoClassDefFoundError("startup failed");
    }

    @Override
    public void shutdown () {

    }
  }

  // Holds startup() open for a fixed time under a two-second keep-alive cadence and a caller-chosen fail-safe
  // ceiling (null leaves the inherited lookup in place).
  private static class HoldingListener extends AbstractWrapperListener {

    private final Integer failSafeTimeoutSeconds;
    private final long holdMillis;

    private HoldingListener (Integer failSafeTimeoutSeconds, long holdMillis) {

      this.failSafeTimeoutSeconds = failSafeTimeoutSeconds;
      this.holdMillis = holdMillis;
    }

    @Override
    protected int getStartupTimeoutSeconds () {

      return 2;
    }

    @Override
    protected int getStartupCeilingSeconds () {

      return (failSafeTimeoutSeconds == null) ? super.getStartupCeilingSeconds() : failSafeTimeoutSeconds;
    }

    @Override
    public void startup (String[] args)
      throws Exception {

      Thread.sleep(holdMillis);
    }

    @Override
    public void shutdown () {

    }
  }

  private static class RecordingListener extends AbstractWrapperListener {

    private final boolean startupThrows;
    private final int awaitedSignals;
    private final Integer startupTimeoutSeconds;
    private String[] startupArgs;

    private RecordingListener (boolean startupThrows) {

      this(startupThrows, 0, null);
    }

    // awaitedSignals > 0 makes startup() block (up to five seconds) until the keep-alive has fired that many times;
    // a non-null startupTimeoutSeconds replaces the wrapper.startup.timeout lookup
    private RecordingListener (boolean startupThrows, int awaitedSignals, Integer startupTimeoutSeconds) {

      this.startupThrows = startupThrows;
      this.awaitedSignals = awaitedSignals;
      this.startupTimeoutSeconds = startupTimeoutSeconds;
    }

    @Override
    protected int getStartupTimeoutSeconds () {

      return (startupTimeoutSeconds == null) ? super.getStartupTimeoutSeconds() : startupTimeoutSeconds;
    }

    @Override
    public void startup (String[] args)
      throws Exception {

      startupArgs = args;

      if (awaitedSignals > 0) {

        long deadlineMillis = System.currentTimeMillis() + 5000;

        while ((WrapperManager.signalStartingCount() < awaitedSignals) && (System.currentTimeMillis() < deadlineMillis)) {
          Thread.sleep(50);
        }
      }
      if (startupThrows) {
        throw new Exception("startup failed");
      }
    }

    @Override
    public void shutdown () {

    }
  }
}
