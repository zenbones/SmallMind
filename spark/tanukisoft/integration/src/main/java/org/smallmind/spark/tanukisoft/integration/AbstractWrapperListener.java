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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Bridge between Tanuki's Java Service Wrapper and a SmallMind application. Subclasses supply concrete
 * {@link #startup(String[])} and {@link #shutdown()} behavior; this class takes care of:
 * <ul>
 *   <li>reflectively instantiating the configured listener from {@code main} and handing it to {@link WrapperManager};</li>
 *   <li>owning a {@link PerApplicationScope}, available through {@link #getPerApplicationScope()}, and running
 *       {@link #startup(String[])}, {@link #shutdown()}, and {@link #controlEvent(int)} inside it; threads the
 *       application starts inherit no binding and must be built from {@code PerApplicationScope.wrapThreadFactory}
 *       or run their work through an {@code ApplicationCarrier} (the keep-alive executor below never resolves
 *       per-application data and is deliberately left unwrapped);</li>
 *   <li>scheduling a background "keep-alive" task on a single-thread scheduled executor that calls
 *       {@link WrapperManager#signalStarting(int)}, promising the startup timeout ({@code wrapper.startup.timeout}
 *       in {@code wrapper.conf}, converted to the milliseconds the wrapper expects) as soon as
 *       {@link #start(String[])} begins and then at half that timeout, so slow startups are not killed by the
 *       wrapper watchdog
 *       (the task stops signalling and cancels itself once the fail-safe ceiling, {@code spark.startup.ceiling} in
 *       {@code wrapper.conf} or 180 seconds, is reached);</li>
 *   <li>translating wrapper control events into {@link WrapperManager#stop(int)} calls when running without the
 *       native wrapper;</li>
 *   <li>converting exceptions thrown by {@link #startup(String[])} or {@link #shutdown()} into stack-trace exit codes.</li>
 * </ul>
 */
public abstract class AbstractWrapperListener extends PerApplicationContext implements WrapperListener {

  private static final String STARTUP_TIMEOUT_PROPERTY = "wrapper.startup.timeout";
  private static final String STARTUP_CEILING_PROPERTY = "spark.startup.ceiling";
  private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_STARTUP_CEILING_SECONDS = 180;
  private static final int STACK_TRACE_ERROR_CODE = 2;
  private static final int UNLIMITED_STARTUP_TIMEOUT_SECONDS = 0;
  private static final int MINIMUM_STARTUP_TIMEOUT_SECONDS = 10;

  private final PerApplicationScope perApplicationScope = new PerApplicationScope();

  /**
   * Entry point invoked by the wrapper. The first argument must name the concrete {@link WrapperListener} subclass
   * to instantiate; remaining arguments are expanded via {@link PropertyExpander} so that
   * {@code ${system.property}}-style placeholders work, and are then handed, with the listener, to
   * {@link WrapperManager#start(WrapperListener, String[])}. The wrapper configuration is deliberately not consulted
   * here: {@link WrapperManager#getProperties()} is only populated once the JVM has connected to the native wrapper,
   * which happens inside {@code WrapperManager.start}, so the startup timeout and fail-safe ceiling are read later,
   * by {@link #start(String[])}.
   *
   * @param args the first element is the fully qualified listener class name; subsequent elements are passed to its
   *             {@link #startup(String[])}
   * @throws IllegalArgumentException if no listener class name is provided
   * @throws Throwable                if reflectively instantiating the listener or handing it to the wrapper fails
   */
  static void main (String... args)
    throws Throwable {

    if (args.length < 1) {
      throw new IllegalArgumentException(String.format("First application parameter must be the class of the %s in use", WrapperListener.class.getSimpleName()));
    } else {

      String[] expandedArgs = new String[args.length - 1];
      PropertyExpander propertyExpander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, true);

      for (int count = 1; count < args.length; count++) {
        expandedArgs[count - 1] = propertyExpander.expand(args[count]);
      }

      WrapperManager.start((WrapperListener)Class.forName(args[0]).getConstructor().newInstance(), expandedArgs);
    }
  }

  private static Integer parseStartupCeilingProperty () {

    String ceilingSeconds;

    if ((ceilingSeconds = WrapperManager.getProperties().getProperty(STARTUP_CEILING_PROPERTY)) == null) {

      return null;
    }
    try {
      return Integer.parseInt(ceilingSeconds.trim());
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalStateException(String.format("Unable to parse the property(%s) as an integer(%s)", STARTUP_CEILING_PROPERTY, ceilingSeconds));
    }
  }

  /**
   * Hook for subclasses to perform application startup. Invoked on the wrapper start thread.
   *
   * @param args application arguments, after the wrapper-specific timeout prefix has been removed
   * @throws Exception if application startup fails; the failure is turned into a stack-trace exit code
   */
  public abstract void startup (String[] args)
    throws Exception;

  /**
   * Hook for subclasses to perform application shutdown. Invoked on the wrapper stop thread.
   *
   * @throws Exception if application shutdown fails; the failure is turned into a stack-trace exit code
   */
  public abstract void shutdown ()
    throws Exception;

  /**
   * The {@link PerApplicationScope} this listener binds around {@link #startup(String[])}, {@link #shutdown()}, and
   * {@link #controlEvent(int)}. Subclasses hand it to whatever needs the same instance — a Spring assembly that
   * wires it into a web server's {@code perApplicationScope} property, or a pool built from
   * {@code PerApplicationScope.wrapThreadFactory} — so that every reader resolves through one map.
   *
   * @return the scope owned by this listener
   */
  protected PerApplicationScope getPerApplicationScope () {

    return perApplicationScope;
  }

  /**
   * Receives wrapper control events, with the listener's {@link PerApplicationScope} bound. When running without
   * the native wrapper, interpret Ctrl-C, Close, and Shutdown as a request to stop the application; otherwise the
   * native wrapper itself handles these events.
   *
   * @param event one of the {@code WrapperManager.WRAPPER_CTRL_*} constants
   */
  public void controlEvent (int event) {

    perApplicationScope.run(() -> boundControlEvent(event));
  }

  private void boundControlEvent (int event) {

    if (!WrapperManager.isControlledByNativeWrapper()) {
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  /**
   * The wrapper's startup timeout in seconds, which is also the window each keep-alive signal promises. Taken from
   * the {@code wrapper.startup.timeout} property in {@code wrapper.conf} when present, otherwise {@code 30}, the
   * wrapper's own default. The property is only visible once the JVM has connected to the native wrapper, which is
   * the case by the time {@link #start(String[])} runs; listeners that want a different policy override this, which
   * replaces the property lookup. A value of zero means the wrapper has been told to wait indefinitely, so no
   * keep-alive is scheduled.
   *
   * @return the timeout in seconds, or zero for no timeout
   * @throws IllegalStateException if {@code wrapper.startup.timeout} is set but unparsable, or is neither {@code 0}
   *                               nor at least {@code 10}
   */
  protected int getStartupTimeoutSeconds () {

    String startupTimeoutSeconds;
    int parsedTimeoutSeconds;

    if ((startupTimeoutSeconds = WrapperManager.getProperties().getProperty(STARTUP_TIMEOUT_PROPERTY)) == null) {

      return DEFAULT_STARTUP_TIMEOUT_SECONDS;
    }
    try {
      parsedTimeoutSeconds = Integer.parseInt(startupTimeoutSeconds.trim());
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalStateException(String.format("Unable to parse the property(%s) as an integer(%s)", STARTUP_TIMEOUT_PROPERTY, startupTimeoutSeconds));
    }
    if ((parsedTimeoutSeconds != UNLIMITED_STARTUP_TIMEOUT_SECONDS) && (parsedTimeoutSeconds < MINIMUM_STARTUP_TIMEOUT_SECONDS)) {
      throw new IllegalStateException(String.format("The property(%s) should be %s >= %d, or %d to have the wrapper wait indefinitely", STARTUP_TIMEOUT_PROPERTY, startupTimeoutSeconds, MINIMUM_STARTUP_TIMEOUT_SECONDS, UNLIMITED_STARTUP_TIMEOUT_SECONDS));
    }

    return parsedTimeoutSeconds;
  }

  /**
   * Fail-safe ceiling, in seconds, after which the keep-alive stops signalling even though {@link #startup(String[])}
   * has not returned, leaving the wrapper free to time out and stop a stalled startup. The wrapper only acts once
   * the window granted by the last signal ({@code wrapper.startup.timeout} seconds) has also passed, so the longest
   * startup that survives is roughly the ceiling plus the timeout. Taken from the {@code spark.startup.ceiling}
   * property in {@code wrapper.conf} when present, otherwise {@code 180}; listeners that want a different policy
   * override this, which replaces the property lookup. A value of zero or less removes the ceiling, so the
   * keep-alive runs until {@code startup} returns or throws.
   *
   * @return the ceiling in seconds, or a non-positive value for no ceiling
   * @throws IllegalStateException if {@code spark.startup.ceiling} is set but unparsable
   */
  protected int getStartupCeilingSeconds () {

    Integer configuredCeilingSeconds;

    return ((configuredCeilingSeconds = parseStartupCeilingProperty()) == null) ? DEFAULT_STARTUP_CEILING_SECONDS : configuredCeilingSeconds;
  }

  /**
   * Wrapper start callback. Reads the startup timeout via {@link #getStartupTimeoutSeconds()}, opens a single-thread
   * scheduled executor (its worker is a daemon named {@code spark-wrapper-signal}) in a try-with-resources block,
   * schedules a "still starting" signaller on it via {@link ScheduledExecutorService#scheduleWithFixedDelay}, and
   * invokes {@link #startup(String[])} on the calling thread. The signaller fires immediately and then every
   * {@code startupTimeoutSeconds / 2} seconds (never less than one second), calling
   * {@link WrapperManager#signalStarting(int)} on each firing with the timeout converted to milliseconds, which is
   * the unit the wrapper reads the wait hint in; the wrapper's startup clock runs from JVM launch rather than from
   * this callback, so the immediate signal resets it before it can expire and the half-timeout cadence keeps it from
   * expiring while startup runs. A {@link RuntimeException} thrown by a signal is printed and the next firing still
   * happens, since a scheduled task that throws would otherwise stop being rescheduled without any trace. Once
   * {@link #getStartupCeilingSeconds()} elapses on the monotonic clock the signaller prints a notice to stderr,
   * stops signalling and cancels its own future, after which the wrapper is free to time out and stop a stalled
   * startup. A startup timeout of {@code 0} means the wrapper has been told to wait indefinitely, so no signaller is
   * scheduled at all. When {@link #startup(String[])} returns or throws, the try-with-resources {@code close()} shuts
   * the executor down. The whole of {@code start} runs inside this listener's {@link PerApplicationScope}; the
   * signaller's thread factory is not wrapped, because the keep-alive task only talks to the wrapper and never
   * resolves per-application data.
   *
   * @param args the application arguments, forwarded unchanged to {@link #startup(String[])}
   * @return {@code null} on success, or {@code 2} if the startup timeout or fail-safe ceiling is misconfigured or
   * {@link #startup(String[])} threw anything, {@link Error} included; {@code null} signals the wrapper to keep
   * running
   */
  public Integer start (String[] args) {

    return perApplicationScope.call(() -> boundStart(args));
  }

  private Integer boundStart (String[] args) {

    try (ScheduledExecutorService signalExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> {

      Thread thread = new Thread(runnable, "spark-wrapper-signal");

      thread.setDaemon(true);

      return thread;
    })) {

      int startupTimeoutSeconds = getStartupTimeoutSeconds();

      if (startupTimeoutSeconds != UNLIMITED_STARTUP_TIMEOUT_SECONDS) {

        AtomicReference<ScheduledFuture<?>> signalFutureRef = new AtomicReference<>();
        int signalIntervalSeconds = Math.max(1, startupTimeoutSeconds / 2);
        int waitHintMillis = Math.toIntExact(TimeUnit.SECONDS.toMillis(startupTimeoutSeconds));
        int failSafeTimeoutSeconds = getStartupCeilingSeconds();
        long startNanos = System.nanoTime();

        signalFutureRef.set(signalExecutor.scheduleWithFixedDelay(() -> {
          if ((failSafeTimeoutSeconds <= 0) || (System.nanoTime() - startNanos < TimeUnit.SECONDS.toNanos(failSafeTimeoutSeconds))) {
            try {
              WrapperManager.signalStarting(waitHintMillis);
            } catch (RuntimeException runtimeException) {
              runtimeException.printStackTrace();
            }
          } else {

            ScheduledFuture<?> signalFuture;

            System.err.println(String.format("Startup has run past the fail-safe ceiling of %d seconds; no further keep-alive signals will be sent, so the wrapper may stop this JVM once its startup timeout of %d seconds expires", failSafeTimeoutSeconds, startupTimeoutSeconds));

            if ((signalFuture = signalFutureRef.get()) != null) {
              signalFuture.cancel(false);
            }
          }
        }, 0, signalIntervalSeconds, TimeUnit.SECONDS));
      }

      startup(args);
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return null;
  }

  /**
   * Wrapper stop callback. Invokes {@link #shutdown()} inside this listener's {@link PerApplicationScope} and
   * converts any {@link Throwable} thrown during shutdown into a distinguishable exit code.
   *
   * @param exitCode the exit code the wrapper wants to use
   * @return {@code exitCode} if shutdown succeeded; {@code 2} if shutdown threw
   */
  public int stop (int exitCode) {

    return perApplicationScope.call(() -> boundStop(exitCode));
  }

  private int boundStop (int exitCode) {

    try {
      shutdown();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return exitCode;
  }
}
