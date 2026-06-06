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
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Bridge between Tanuki's Java Service Wrapper and a SmallMind application. Subclasses supply concrete
 * {@link #startup(String[])} and {@link #shutdown()} behavior; this class takes care of:
 * <ul>
 *   <li>reflectively instantiating the configured listener from {@code main} and handing it to {@link WrapperManager};</li>
 *   <li>scheduling a background "keep-alive" task on a single-thread scheduled executor that repeatedly calls
 *       {@link WrapperManager#signalStarting(int)} so slow startups are not killed by the wrapper watchdog
 *       (the task stops signalling and cancels itself once a fail-safe ceiling is reached);</li>
 *   <li>translating wrapper control events into {@link WrapperManager#stop(int)} calls when running without the
 *       native wrapper;</li>
 *   <li>converting exceptions thrown by {@link #startup(String[])} or {@link #shutdown()} into stack-trace exit codes.</li>
 * </ul>
 */
public abstract class AbstractWrapperListener extends PerApplicationContext implements WrapperListener {

  private final static String DEFAULT_TIMEOUT_SECONDS = "30";
  private static final int STACK_TRACE_ERROR_CODE = 2;
  private static final int MINIMUM_STARTUP_TIMEOUT_SECONDS = 10;
  private static final int FAIL_SAFE_TIMEOUT_SECONDS = 180;

  /**
   * Entry point invoked by the wrapper. The first argument must name the concrete {@link WrapperListener} subclass
   * to instantiate; remaining arguments are expanded via {@link PropertyExpander} so that
   * {@code ${system.property}}-style placeholders work, and the effective wrapper startup timeout is prepended for
   * later consumption by {@link #start(String[])}.
   *
   * @param args the first element is the fully qualified listener class name; subsequent elements are passed to its
   *             {@link #startup(String[])} after the timeout prefix is stripped again
   * @throws IllegalArgumentException if no listener class name is provided
   * @throws IllegalStateException    if {@code wrapper.startup.timeout} is set but unparsable or smaller than the
   *                                  allowed minimum
   * @throws Throwable                if reflectively instantiating the listener or handing it to the wrapper fails
   */
  static void main (String... args)
    throws Throwable {

    if (args.length < 1) {
      throw new IllegalArgumentException(String.format("First application parameter must be the class of the %s in use", WrapperListener.class.getSimpleName()));
    } else {

      String[] modifiedArgs;
      PropertyExpander propertyExpander;
      String startupTimeoutSeconds;

      if ((startupTimeoutSeconds = WrapperManager.getProperties().getProperty("wrapper.startup.timeout")) != null) {
        try {
          if (Integer.parseInt(startupTimeoutSeconds) < MINIMUM_STARTUP_TIMEOUT_SECONDS) {
            throw new IllegalStateException(String.format("The property(wrapper.startup.timeout) should be %s >= %d", startupTimeoutSeconds, MINIMUM_STARTUP_TIMEOUT_SECONDS));
          }
        } catch (NumberFormatException numberFormatException) {
          throw new IllegalStateException(String.format("Unable to parse the property(wrapper.startup.timeout) as in integer(%s)", startupTimeoutSeconds));
        }
      }

      modifiedArgs = new String[args.length];
      modifiedArgs[0] = (startupTimeoutSeconds == null) ? DEFAULT_TIMEOUT_SECONDS : startupTimeoutSeconds;
      System.arraycopy(args, 1, modifiedArgs, 1, args.length - 1);

      propertyExpander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, true);
      for (int count = 0; count < modifiedArgs.length; count++) {
        modifiedArgs[count] = propertyExpander.expand(modifiedArgs[count]);
      }

      WrapperManager.start((WrapperListener)Class.forName(args[0]).getConstructor().newInstance(), modifiedArgs);
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
   * Receives wrapper control events. When running without the native wrapper, interpret Ctrl-C, Close, and Shutdown
   * as a request to stop the application; otherwise the native wrapper itself handles these events.
   *
   * @param event one of the {@code WrapperManager.WRAPPER_CTRL_*} constants
   */
  public void controlEvent (int event) {

    if (!WrapperManager.isControlledByNativeWrapper()) {
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  /**
   * Wrapper start callback. Opens a single-thread scheduled executor (its worker is a daemon named
   * {@code spark-wrapper-signal}) in a try-with-resources block, schedules a "still starting" signaller on it via
   * {@link ScheduledExecutorService#scheduleWithFixedDelay}, and invokes {@link #startup(String[])} on the calling
   * thread. The signaller calls {@link WrapperManager#signalStarting(int)} on each firing so the wrapper watchdog
   * keeps waiting while startup runs; once {@link #FAIL_SAFE_TIMEOUT_SECONDS} elapses it stops signalling and
   * cancels its own future, after which the wrapper is free to time out and stop a stalled startup. When
   * {@link #startup(String[])} returns or throws, the try-with-resources {@code close()} shuts the executor down.
   *
   * @param args arguments where {@code args[0]} is the startup timeout in seconds and the remainder are the
   *             application arguments forwarded to {@link #startup(String[])}
   * @return {@code null} on success, or {@code 2} if {@link #startup(String[])} threw; {@code null} signals the
   * wrapper to keep running
   */
  public Integer start (String[] args) {

    try (ScheduledExecutorService signalExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> {

      Thread thread = new Thread(runnable, "spark-wrapper-signal");

      thread.setDaemon(true);

      return thread;
    })) {

      AtomicReference<ScheduledFuture<?>> signalFutureRef = new AtomicReference<>();
      String[] trimmedArgs = new String[args.length - 1];
      int startupTimeoutSeconds = Integer.parseInt(args[0]);
      long startMillis = System.currentTimeMillis();

      System.arraycopy(args, 1, trimmedArgs, 0, args.length - 1);

      signalFutureRef.set(signalExecutor.scheduleWithFixedDelay(() -> {
        if (System.currentTimeMillis() - startMillis < (FAIL_SAFE_TIMEOUT_SECONDS * 1000L)) {
          WrapperManager.signalStarting(startupTimeoutSeconds);
        } else {

          ScheduledFuture<?> signalFuture;

          if ((signalFuture = signalFutureRef.get()) != null) {
            signalFuture.cancel(false);
          }
        }
      }, startupTimeoutSeconds - 2, startupTimeoutSeconds - 2, TimeUnit.SECONDS));

      startup(trimmedArgs);
    } catch (Exception exception) {
      exception.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return null;
  }

  /**
   * Wrapper stop callback. Invokes {@link #shutdown()} and converts any {@link Throwable} thrown during shutdown
   * into a distinguishable exit code.
   *
   * @param exitCode the exit code the wrapper wants to use
   * @return {@code exitCode} if shutdown succeeded; {@code 2} if shutdown threw
   */
  public int stop (int exitCode) {

    try {
      shutdown();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return exitCode;
  }
}
