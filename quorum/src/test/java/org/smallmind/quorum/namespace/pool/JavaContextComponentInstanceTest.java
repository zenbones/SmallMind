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
package org.smallmind.quorum.namespace.pool;

import java.util.Collections;
import java.util.List;
import javax.naming.CommunicationException;
import javax.naming.Name;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.quorum.namespace.PooledJavaContext;
import org.smallmind.quorum.namespace.PooledJavaContextTestKit;
import org.smallmind.quorum.namespace.PooledJavaContextTestKit.ControllableBackingContext;
import org.smallmind.quorum.namespace.PooledJavaContextTestKit.LookupBehavior;
import org.smallmind.quorum.pool.complex.AbstractComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Drives the {@link JavaContextComponentInstance} branches the LDAP integration test does not cover —
 * validation against a dead context, the communication-abort path (terminate-then-report, including
 * its propagation of the originating exception to the pool's listeners), existential stack-trace
 * capture on serve, and the idempotent forced close — using a programmable backing context rather
 * than a live directory server. The pool is real but never started; lifecycle calls against it are
 * harmless no-ops save for the listener fan-out the assertions rely on.
 */
@Test(groups = "unit")
public class JavaContextComponentInstanceTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    // terminateInstance updates Claxon size metrics, which read a PerApplicationContext; install an
    // empty one so Instrument resolves to its no-op.
    new PerApplicationContext();
  }

  private ComponentPool<PooledJavaContext> pool (boolean existentiallyAware) {

    return new ComponentPool<>("namespace-instance", new NoOpFactory(), new ComplexPoolConfig().setExistentiallyAware(existentiallyAware));
  }

  public void testValidateReturnsTrueWhenTheBackingContextResponds ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool(false), PooledJavaContextTestKit.pooledContextOver(backing, false));

    Assert.assertTrue(instance.validate(), "a context whose no-op lookup succeeds should validate");
  }

  public void testValidateReturnsFalseWhenTheBackingContextFails ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();

    backing.setLookupBehavior(LookupBehavior.THROW_NAMING);

    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool(false), PooledJavaContextTestKit.pooledContextOver(backing, false));

    Assert.assertFalse(instance.validate(), "a context whose validation lookup throws should be reported unusable");
  }

  public void testCommunicationFailureAbortsTerminatesAndReportsTheError ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();
    PooledJavaContext pooledJavaContext = PooledJavaContextTestKit.pooledContextOver(backing, false);
    ComponentPool<PooledJavaContext> pool = pool(false);
    RecordingErrorListener listener = new RecordingErrorListener();

    pool.addComponentPoolEventListener(listener);

    // Registering the instance as a context listener wires the abort path; a failing operation then
    // makes the pooled context fire contextAborted into this instance. The strong reference is kept
    // deliberately, since the context holds its listeners weakly.
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool, pooledJavaContext);

    backing.setLookupBehavior(LookupBehavior.THROW_COMMUNICATION);

    // The Name overload is invoked directly: the String overload would delegate through the polymorphic
    // Name overload and fire the abort on both frames, double-reporting the error.
    Name name = pooledJavaContext.getNameParser("ignored").parse("anything");
    CommunicationException thrown = Assert.expectThrows(CommunicationException.class, () -> pooledJavaContext.lookup(name));

    Assert.assertNotNull(instance, "the instance is held strongly so the weak listener list does not drop it");
    Assert.assertEquals(listener.getErrorEvents().size(), 1, "an aborted context should report exactly one error to the pool");
    Assert.assertSame(listener.getErrorEvents().get(0).getException(), thrown, "the originating communication failure should be the reported exception");
  }

  public void testServeCapturesAStackTraceWhenExistentiallyAware ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool(true), PooledJavaContextTestKit.pooledContextOver(backing, false));

    Assert.assertNotNull(instance.serve(), "serve should hand back the wrapped context");
    Assert.assertNotNull(instance.getExistentialStackTrace(), "an existentially aware pool should record the acquiring stack trace");
  }

  public void testServeDoesNotCaptureAStackTraceWhenNotAware ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool(false), PooledJavaContextTestKit.pooledContextOver(backing, false));

    instance.serve();

    Assert.assertNull(instance.getExistentialStackTrace(), "with awareness off no stack trace should be recorded");
  }

  public void testForcedCloseIsIdempotentAndPhysicallyClosesOnce ()
    throws Exception {

    ControllableBackingContext backing = new ControllableBackingContext();
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool(false), PooledJavaContextTestKit.pooledContextOver(backing, false));

    instance.close();
    instance.close();

    Assert.assertEquals(backing.closes(), 1, "the atomic guard should physically close the backing context at most once");
  }

  public void testAbortReportsTheTerminationFailureWhenTerminateInstanceThrows ()
    throws Exception {

    // When the pool's own terminateInstance fails, contextAborted must still report exactly one error —
    // and the termination failure (not the original communication exception) becomes the reported one.
    RuntimeException terminationFailure = new RuntimeException("the pool refused to terminate the instance");
    TerminateFailingPool pool = new TerminateFailingPool(terminationFailure);
    RecordingErrorListener listener = new RecordingErrorListener();

    pool.addComponentPoolEventListener(listener);

    ControllableBackingContext backing = new ControllableBackingContext();
    PooledJavaContext pooledJavaContext = PooledJavaContextTestKit.pooledContextOver(backing, false);
    JavaContextComponentInstance instance = new JavaContextComponentInstance(pool, pooledJavaContext);

    backing.setLookupBehavior(LookupBehavior.THROW_COMMUNICATION);

    Name name = pooledJavaContext.getNameParser("ignored").parse("anything");
    Assert.assertThrows(CommunicationException.class, () -> pooledJavaContext.lookup(name));

    Assert.assertNotNull(instance, "the instance is held strongly so the weak listener list does not drop it");
    Assert.assertEquals(listener.getErrorEvents().size(), 1, "a failed termination must still report exactly one error");
    Assert.assertSame(listener.getErrorEvents().get(0).getException(), terminationFailure, "the termination failure should supersede the communication exception as the reported error");
  }

  // A pool whose terminateInstance always fails, used to drive the abort path's termination-failure
  // arm. createInstance is never reached, since the pool is never started.
  private static class TerminateFailingPool extends ComponentPool<PooledJavaContext> {

    private final RuntimeException failure;

    private TerminateFailingPool (RuntimeException failure) {

      super("terminate-fails", new NoOpFactory());

      this.failure = failure;
    }

    @Override
    public void terminateInstance (ComponentInstance<PooledJavaContext> componentInstance) {

      throw failure;
    }
  }

  // A factory whose createInstance is never invoked (the pool is never started); it exists only to
  // satisfy the ComponentPool constructor.
  private static class NoOpFactory extends AbstractComponentInstanceFactory<PooledJavaContext> {

    @Override
    public ComponentInstance<PooledJavaContext> createInstance (ComponentPool<PooledJavaContext> componentPool) {

      throw new UnsupportedOperationException("the pool under test is never started");
    }
  }

  private static class RecordingErrorListener implements ComponentPoolEventListener {

    private final List<ErrorReportingComponentPoolEvent<?>> errorEvents = Collections.synchronizedList(new java.util.LinkedList<>());

    private List<ErrorReportingComponentPoolEvent<?>> getErrorEvents () {

      return errorEvents;
    }

    @Override
    public void reportErrorOccurred (ErrorReportingComponentPoolEvent<?> event) {

      errorEvents.add(event);
    }

    @Override
    public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent<?> event) {

    }
  }
}
