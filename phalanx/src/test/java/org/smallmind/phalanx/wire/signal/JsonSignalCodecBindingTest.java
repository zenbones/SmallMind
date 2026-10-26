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
package org.smallmind.phalanx.wire.signal;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.phalanx.wire.WireTestingService;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.DatabindException;

/**
 * Decodes an {@link InvocationSignal} carrying a {@link WireContext} straight through the {@link JsonSignalCodec}, the
 * way the response transports do, to pin down what surfaces when the decoding thread has no
 * {@link PerApplicationContext} to resolve the context handle against.
 */
@Test(groups = "unit")
public class JsonSignalCodecBindingTest {

  // The handle lookup fails inside the XmlAdapter, where Jackson wraps a runtime failure (WRAP_EXCEPTIONS is on by
  // default) with the property path, so the codec surfaces a DatabindException whose cause chain carries the
  // missing-context exception rather than that exception itself.
  @Test
  public void testDecodingAContextWithNoContextOnTheThreadSurfacesTheMissingContextThroughJackson ()
    throws Exception {

    byte[] bytes = encodeSignalCarryingAContext();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        new JsonSignalCodec().decode(bytes, 0, bytes.length, InvocationSignal.class);
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    Assert.assertNotNull(failure.get(), "decoding a wire context with no context on the thread must fail");
    Assert.assertTrue(failure.get() instanceof DatabindException, "expected Jackson to wrap the failure, saw " + failure.get());
    Assert.assertNotNull(findInChain(failure.get(), MissingPerApplicationContextException.class), "the cause chain must carry the missing-context exception, saw " + failure.get());
  }

  @Test
  public void testDecodingAContextWithTheHandleRegisteredYieldsTheRegisteredClass ()
    throws Exception {

    byte[] bytes = encodeSignalCarryingAContext();
    AtomicReference<InvocationSignal> decoded = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        new PerApplicationContext();
        WireContextManager.register("test", TestWireContext.class);
        decoded.set(new JsonSignalCodec().decode(bytes, 0, bytes.length, InvocationSignal.class));
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    Assert.assertNull(failure.get(), "decoding with the handle registered must succeed, saw " + failure.get());
    Assert.assertEquals(decoded.get().getContexts().length, 1);
    Assert.assertTrue(decoded.get().getContexts()[0] instanceof TestWireContext, "expected the registered class, saw " + decoded.get().getContexts()[0]);
    Assert.assertEquals(((TestWireContext)decoded.get().getContexts()[0]).getMessage(), "flibble");
  }

  private static byte[] encodeSignalCarryingAContext ()
    throws NoSuchMethodException {

    return new JsonSignalCodec().encode(new InvocationSignal(false, new Route(1, "WireTestService", new Function(WireTestingService.class.getMethod("hasContext"))), new HashMap<>(), new WireContext[] {new TestWireContext("flibble")}, null));
  }

  private static Throwable findInChain (Throwable throwable, Class<? extends Throwable> expectedClass) {

    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (expectedClass.isInstance(cause)) {

        return cause;
      }
      if (cause.getCause() == cause) {
        break;
      }
    }

    return null;
  }
}
