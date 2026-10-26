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
package org.smallmind.phalanx.wire.transport.mock;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeFactory;
import org.smallmind.phalanx.CapturingAppender;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.TestWireScope;
import org.smallmind.phalanx.wire.TransportTimeoutException;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Proves what a caller sees when the receiving side can not decode the binding flavor the call carries. A wire
 * context arriving on a dispatch thread with no {@code PerApplicationContext}, or a wire scope arriving on a router
 * built without a {@link PerApplicationScope}, fails in the codec; the mock transport logs the failure at
 * {@code ERROR} and drops the request, so the service is never invoked, no result is ever published, and the caller
 * runs out its timeout. The service interface declares no checked exceptions, so the proxy surfaces the
 * {@link TransportTimeoutException} wrapped in an {@link UndeclaredThrowableException}.
 */
@Test(groups = "unit")
public class MockReceiverBindingMismatchTest {

  private static final long SHORT_TIMEOUT_SECONDS = 1;
  private static final long LOG_WAIT_MILLIS = 3000;

  // The dispatch threads are created by the router, so a router built on a thread with no context yields a
  // receiver that has nothing to resolve wire context handles against.
  @Test
  public void testWireContextIsDroppedWhenTheReceiverThreadsHaveNoContext ()
    throws Exception {

    MockMessageRouter messageRouter = onIsolatedThread(() -> new MockMessageRouter(new PerApplicationScope()));

    assertRequestIsDropped(messageRouter, MissingPerApplicationContextException.class, (wireTestingService) -> {
      ContextFactory.pushContext(new TestWireContext("flibble"));
      try {
        wireTestingService.hasContext();
      } finally {
        ContextFactory.popContext(TestWireContext.class);
      }
    });
  }

  @Test
  public void testWireScopeIsDroppedWhenTheRouterHasNoScope ()
    throws Exception {

    assertRequestIsDropped(new MockMessageRouter(null), MissingPerApplicationScopeException.class, (wireTestingService) -> ScopeFactory.runInScope(new TestWireScope("flibble"), wireTestingService::hasScope));
  }

  private static boolean chainContains (Throwable throwable, Class<? extends Throwable> expectedClass) {

    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (expectedClass.isInstance(cause)) {

        return true;
      }
      if (cause.getCause() == cause) {
        break;
      }
    }

    return false;
  }

  private static void assertRequestIsDropped (MockMessageRouter messageRouter, Class<? extends Throwable> expectedCause, Consumer<WireTestingService> invocation)
    throws Exception {

    JsonSignalCodec signalCodec = new JsonSignalCodec();
    MockResponseTransport responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    MockRequestTransport requestTransport = new MockRequestTransport(messageRouter, signalCodec, SHORT_TIMEOUT_SECONDS);
    CapturingAppender appender = CapturingAppender.attach(MockResponseTransport.class, Level.ERROR);

    try {

      CountingWireTestingService service = new CountingWireTestingService();
      WireTestingService wireTestingService;
      Record<?> loggedRecord;

      responseTransport.register(WireTestingService.class, service);
      wireTestingService = (WireTestingService)WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("test"), null, null);

      try {
        invocation.accept(wireTestingService);
        Assert.fail("the receiver can not decode this call, so no result should ever reach the caller");
      } catch (UndeclaredThrowableException undeclaredThrowableException) {
        Assert.assertTrue(undeclaredThrowableException.getCause() instanceof TransportTimeoutException, "the dropped request must surface as a timeout, not: " + undeclaredThrowableException.getCause());
      }

      Assert.assertTrue(appender.awaitRecord(LOG_WAIT_MILLIS), "the receiver must log the decode failure it dropped");

      loggedRecord = appender.getRecords().get(0);
      Assert.assertEquals(loggedRecord.getLevel(), Level.ERROR);
      Assert.assertNotNull(loggedRecord.getThrown(), "the logged record must carry the decode failure");
      Assert.assertTrue(chainContains(loggedRecord.getThrown(), expectedCause), "the logged failure must stem from " + expectedCause.getSimpleName() + ", saw " + loggedRecord.getThrown());
      Assert.assertEquals(service.invocationCount(), 0, "a request dropped in the codec must never reach the service");
    } finally {
      CapturingAppender.release(MockResponseTransport.class);
      requestTransport.close();
      responseTransport.close();
      messageRouter.close();
    }
  }

  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }

    return result.get();
  }

  // Counts the invocations of the two methods these tests call, so a dropped request can be shown never to arrive.
  private static class CountingWireTestingService extends WireTestingServiceImpl {

    private final AtomicInteger invocations = new AtomicInteger(0);

    private int invocationCount () {

      return invocations.get();
    }

    @Override
    public boolean hasContext () {

      invocations.incrementAndGet();

      return super.hasContext();
    }

    @Override
    public boolean hasScope () {

      invocations.incrementAndGet();

      return super.hasScope();
    }
  }
}
