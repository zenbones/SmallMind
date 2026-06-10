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

import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that the no-argument lifecycle convenience methods on {@link AbstractJugglingPin}
 * delegate to their reflective counterparts with a {@code null} hook method, and that the base
 * reflective methods are silent no-ops a subclass can leave untouched.
 */
@Test(groups = "unit")
public class AbstractJugglingPinTest {

  public void testNoArgumentLifecycleDelegatesWithANullMethod ()
    throws JugglerResourceException {

    RecordingPin pin = new RecordingPin();

    pin.start();
    pin.stop();
    pin.close();

    Assert.assertTrue(pin.startCalled, "start() should delegate to start(Method, Object...)");
    Assert.assertTrue(pin.stopCalled, "stop() should delegate to stop(Method, Object...)");
    Assert.assertTrue(pin.closeCalled, "close() should delegate to close(Method, Object...)");
    Assert.assertNull(pin.startMethod, "the convenience overload should pass a null hook method");
    Assert.assertNull(pin.stopMethod);
    Assert.assertNull(pin.closeMethod);
  }

  public void testDefaultReflectiveMethodsAreNoOps ()
    throws JugglerResourceException {

    // A subclass that overrides nothing inherits the no-op bodies; invoking them must not throw.
    DefaultPin pin = new DefaultPin();

    pin.start(null);
    pin.stop(null);
    pin.close(null);
    pin.start();
    pin.stop();
    pin.close();

    Assert.assertEquals(pin.obtain(), "resource");
  }

  private static class RecordingPin extends AbstractJugglingPin<String> {

    private Method startMethod;
    private Method stopMethod;
    private Method closeMethod;
    private boolean startCalled = false;
    private boolean stopCalled = false;
    private boolean closeCalled = false;

    @Override
    public void start (Method method, Object... args) {

      startCalled = true;
      startMethod = method;
    }

    @Override
    public void stop (Method method, Object... args) {

      stopCalled = true;
      stopMethod = method;
    }

    @Override
    public void close (Method method, Object... args) {

      closeCalled = true;
      closeMethod = method;
    }

    @Override
    public String obtain () {

      return "resource";
    }

    @Override
    public boolean recover () {

      return false;
    }

    @Override
    public String describe () {

      return "recording-pin";
    }
  }

  private static class DefaultPin extends AbstractJugglingPin<String> {

    @Override
    public String obtain () {

      return "resource";
    }

    @Override
    public boolean recover () {

      return false;
    }

    @Override
    public String describe () {

      return "default-pin";
    }
  }
}
