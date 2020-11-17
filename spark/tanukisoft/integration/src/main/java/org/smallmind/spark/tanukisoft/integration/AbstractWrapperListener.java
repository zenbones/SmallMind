/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public abstract class AbstractWrapperListener extends PerApplicationContext implements WrapperListener {

  private final static String DEFAULT_TIMEOUT_SECONDS = "30";
  private static final int STACK_TRACE_ERROR_CODE = 2;
  private static final int MINIMUM_STARTUP_TIMEOUT_SECONDS = 10;
  private static final int FAIL_SAFE_TIMEOUT_SECONDS = 180;

  public static void main (String... args)
    throws Throwable {

    if (args.length < 1) {
      throw new IllegalArgumentException(String.format("First application parameter must be the class of the %s in use", WrapperListener.class.getSimpleName()));
    } else {

      String[] modifiedArgs;
      PropertyExpander propertyExpander;
      String startupTimeoutSeconds;

      if ((startupTimeoutSeconds = WrapperManager.getProperties().getProperty("wrapper.startup.timeout")) != null) {
        try {
          if (Integer.parseInt(startupTimeoutSeconds) < 10) {
            throw new IllegalStateException(String.format("The property(wrapper.startup.timeout) should be %s >= 10", MINIMUM_STARTUP_TIMEOUT_SECONDS));
          }
        } catch (NumberFormatException numberFormatException) {
          throw new IllegalStateException(String.format("Unable to parse the property(wrapper.startup.timeout) as in integer(%s)", startupTimeoutSeconds));
        }
      }

      modifiedArgs = new String[args.length];
      modifiedArgs[0] = (startupTimeoutSeconds == null) ? DEFAULT_TIMEOUT_SECONDS : startupTimeoutSeconds;
      System.arraycopy(args, 1, modifiedArgs, 1, args.length - 1);

      propertyExpander = new PropertyExpander(false, SystemPropertyMode.FALLBACK, true);
      for (int count = 0; count < modifiedArgs.length; count++) {
        modifiedArgs[count] = propertyExpander.expand(modifiedArgs[count]);
      }

      WrapperManager.start((WrapperListener)Class.forName(args[0]).newInstance(), modifiedArgs);
    }
  }

  public abstract void startup (String[] args)
    throws Exception;

  public abstract void shutdown ()
    throws Exception;

  public void controlEvent (int event) {

    if (!WrapperManager.isControlledByNativeWrapper()) {
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  public Integer start (String[] args) {

    CountDownLatch completedLatch = new CountDownLatch(1);

    try {

      Thread signalThread;
      String[] trimmedArgs = new String[args.length - 1];
      int startupTimeoutSeconds = Integer.parseInt(args[0]);

      System.arraycopy(args, 1, trimmedArgs, 0, args.length - 1);

      signalThread = new Thread(new SignalWorker(completedLatch, startupTimeoutSeconds));
      signalThread.setDaemon(true);
      signalThread.start();

      startup(trimmedArgs);
    } catch (Exception exception) {
      exception.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    } finally {
      completedLatch.countDown();
    }

    return null;
  }

  public int stop (int exitCode) {

    try {
      shutdown();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return exitCode;
  }

  private static class SignalWorker implements Runnable {

    private final CountDownLatch completedLatch;
    private final int startupTimeoutSeconds;

    public SignalWorker (CountDownLatch completedLatch, int startupTimeoutSeconds) {

      this.completedLatch = completedLatch;
      this.startupTimeoutSeconds = startupTimeoutSeconds;
    }

    @Override
    public void run () {

      long startMillis = System.currentTimeMillis();

      try {
        while ((System.currentTimeMillis() - startMillis < (FAIL_SAFE_TIMEOUT_SECONDS * 1000)) && (!completedLatch.await(startupTimeoutSeconds - 2, TimeUnit.SECONDS))) {
          WrapperManager.signalStarting(startupTimeoutSeconds);
        }
      } catch (InterruptedException interruptedException) {
        // do nothing
      }
    }
  }
}