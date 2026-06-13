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
package org.tanukisoftware.wrapper;

import java.util.Properties;

/**
 * Test double that deliberately shadows {@code org.tanukisoftware.wrapper.WrapperManager} from the wrapper jar.
 * <p>The real class is a thin facade over a native service-wrapper process: its static methods either require the
 * native library or connect to a running wrapper, so the {@code start}, {@code controlEvent}, and {@code main}
 * behaviour of {@code AbstractWrapperListener} cannot otherwise be exercised in a unit test. Because the surefire
 * classpath places {@code target/test-classes} ahead of dependency jars, this copy is the one loaded at test time;
 * it captures interactions for assertion instead of touching any native code.
 * <p>Only the static surface that {@code AbstractWrapperListener} actually calls is provided. The control-event
 * constants carry the same values the real wrapper publishes (and which are inlined into the already-compiled
 * production class), so tests can pass them through faithfully.
 */
public class WrapperManager {

  public static final int WRAPPER_CTRL_C_EVENT = 200;
  public static final int WRAPPER_CTRL_CLOSE_EVENT = 201;
  public static final int WRAPPER_CTRL_LOGOFF_EVENT = 202;
  public static final int WRAPPER_CTRL_SHUTDOWN_EVENT = 203;

  private static Properties properties = new Properties();
  private static boolean controlledByNativeWrapper = false;
  private static int signalStartingCount = 0;
  private static int lastSignalStartingTimeout = -1;
  private static WrapperListener startedListener = null;
  private static String[] startedArguments = null;
  private static Integer lastStopCode = null;

  /**
   * Restores the double to its initial state; intended to be called before each test.
   */
  public static void reset () {

    properties = new Properties();
    controlledByNativeWrapper = false;
    signalStartingCount = 0;
    lastSignalStartingTimeout = -1;
    startedListener = null;
    startedArguments = null;
    lastStopCode = null;
  }

  public static void setControlledByNativeWrapper (boolean controlled) {

    controlledByNativeWrapper = controlled;
  }

  public static int signalStartingCount () {

    return signalStartingCount;
  }

  public static int lastSignalStartingTimeout () {

    return lastSignalStartingTimeout;
  }

  public static WrapperListener startedListener () {

    return startedListener;
  }

  public static String[] startedArguments () {

    return startedArguments;
  }

  public static Integer lastStopCode () {

    return lastStopCode;
  }

  // ---- shadowed surface called by AbstractWrapperListener ----

  public static Properties getProperties () {

    return properties;
  }

  public static boolean isControlledByNativeWrapper () {

    return controlledByNativeWrapper;
  }

  public static void signalStarting (int timeout) {

    signalStartingCount++;
    lastSignalStartingTimeout = timeout;
  }

  public static void start (WrapperListener listener, String[] arguments) {

    startedListener = listener;
    startedArguments = arguments;
  }

  public static void stop (int code) {

    lastStopCode = code;
  }
}
