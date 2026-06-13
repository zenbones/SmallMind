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

import org.tanukisoftware.wrapper.WrapperManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

// Exercises the wrapper-coupled surfaces (start, controlEvent, the main bootstrap) against the shadowed WrapperManager
// double declared under src/test/java/org/tanukisoftware/wrapper. A small startup timeout is used so the background
// "still starting" signaller fires quickly and the start() executor closes promptly.
@org.testng.annotations.Test(groups = "unit")
public class AbstractWrapperListenerLifecycleTest {

  private static class RecordingListener extends AbstractWrapperListener {

    private final boolean startupThrows;
    private String[] startupArgs;

    private RecordingListener (boolean startupThrows) {

      this.startupThrows = startupThrows;
    }

    @Override
    public void startup (String[] args)
      throws Exception {

      startupArgs = args;

      if (startupThrows) {
        throw new Exception("startup failed");
      }
    }

    @Override
    public void shutdown () {

    }
  }

  @BeforeMethod
  public void resetWrapperManager () {

    WrapperManager.reset();
  }

  public void testStartForwardsTrimmedArgumentsAndReportsSuccess () {

    RecordingListener listener = new RecordingListener(false);

    Integer result = listener.start(new String[] {"3", "alpha", "beta"});

    Assert.assertNull(result);
    Assert.assertEquals(listener.startupArgs, new String[] {"alpha", "beta"});
  }

  public void testStartTranslatesStartupFailureIntoTheStackTraceCode () {

    Assert.assertEquals(new RecordingListener(true).start(new String[] {"3", "alpha"}), Integer.valueOf(2));
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

  // With no wrapper.startup.timeout property the default prefix is applied, the listener is instantiated by name, and
  // it is handed to the wrapper along with the (expanded) arguments.
  public void testMainBootstrapsTheListenerWithTheDefaultTimeout ()
    throws Throwable {

    AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha");

    Assert.assertTrue(WrapperManager.startedListener() instanceof CapturingWrapperListener);
    Assert.assertEquals(WrapperManager.startedArguments(), new String[] {"30", "alpha"});
  }

  public void testMainHonoursAnExplicitStartupTimeout ()
    throws Throwable {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "45");

    AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha");

    Assert.assertEquals(WrapperManager.startedArguments(), new String[] {"45", "alpha"});
  }

  public void testMainRejectsAStartupTimeoutBelowTheMinimum () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "5");

    Assert.assertThrows(IllegalStateException.class, () -> AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha"));
  }

  public void testMainRejectsAnUnparsableStartupTimeout () {

    WrapperManager.getProperties().setProperty("wrapper.startup.timeout", "soon");

    Assert.assertThrows(IllegalStateException.class, () -> AbstractWrapperListener.main(CapturingWrapperListener.class.getName(), "alpha"));
  }
}
