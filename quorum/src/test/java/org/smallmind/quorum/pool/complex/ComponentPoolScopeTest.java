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
package org.smallmind.quorum.pool.complex;

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.ClaxonRegistryManager;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Proves that a pool given a {@link PerApplicationScope} reaps components on a deconstruction thread bound to that
 * scope, that a pool given none adopts the scope bound on the thread that starts it, and that with neither it reaps
 * them unbound. The component records what its own thread sees when the pool closes it, so the assertion is on the
 * reaping thread's binding rather than the test thread's.
 */
@Test(groups = "unit")
public class ComponentPoolScopeTest {

  private static final int LIMIT_SECONDS = 1;
  private static final long SETTLE_MILLIS = 2500L;
  private static final long CREATION_TIMEOUT_MILLIS = 5000L;

  private static void startInside (PerApplicationScope perApplicationScope, ComponentPool<String> pool)
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

      try {
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();
    rethrow(failure);
  }

  private static void rethrow (AtomicReference<Throwable> failure)
    throws Exception {

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation resolves through the bridging ClaxonRegistryManager, which falls back to
    // the context flavor; keep a context attached as the other pool tests do, so that only the scope flavor is
    // under test here.
    new PerApplicationContext();
  }

  @Test
  public void testDeconstructionRunsInsideTheScopeWhenOneIsSet ()
    throws ComponentPoolException, InterruptedException {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("scoped-idle", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS)).setPerApplicationScope(perApplicationScope);

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    pool.startup();
    try {
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.seenOnClose.get(), "scoped");
    } finally {
      pool.shutdown();
    }
  }

  // "Both" mode in miniature: the registry is installed on a thread carrying a context, the pool is started there
  // without a scope, and the reaping thread — which inherits only the context — still resolves the registry through
  // the bridging manager.
  @Test
  public void testUnscopedPoolResolvesTheRegistryThroughTheInheritedContext ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

      ClaxonRegistry registry = new ClaxonRegistry(new ClaxonConfiguration());

      try {
        new PerApplicationContext();
        ClaxonRegistryManager.register(registry);

        RegistryRecordingFactory factory = new RegistryRecordingFactory();
        ComponentPool<String> pool = new ComponentPool<>("context-idle", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));

        pool.startup();
        try {
          Thread.sleep(SETTLE_MILLIS);

          Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
          Assert.assertSame(factory.seenOnClose.get(), registry, "the reaping thread must find the registry through the inherited context");
        } finally {
          pool.shutdown();
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      } finally {
        try {
          registry.stop();
        } catch (Exception exception) {
          // nothing more to do
        }
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
  }

  @Test
  public void testDeconstructionIsUnboundWithoutAScope ()
    throws ComponentPoolException, InterruptedException {

    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("unscoped-idle", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertTrue(factory.seenOnClose.get() instanceof MissingPerApplicationScopeException, "expected an unbound reaper, saw " + factory.seenOnClose.get());
    } finally {
      pool.shutdown();
    }
  }

  // A creator thread is started by whichever thread's request triggers the creation, so it inherits that thread's
  // context — here none, even though the pool was started on a thread carrying one — and, like every new thread, no
  // ScopedValue binding, even though the triggering thread is inside one.
  @Test
  public void testCreatorThreadInheritsTheTriggeringThreadsContextButNeverItsScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("creator-inherits", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    pool.startup();
    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

        try {
          Assert.assertFalse(PerApplicationContext.isBound(), "the triggering thread must carry no context");
          perApplicationScope.run(() -> {
            try {
              Assert.assertEquals(pool.getComponent(), "component");
            } catch (ComponentPoolException componentPoolException) {
              throw new RuntimeException(componentPoolException);
            }
          });
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      });

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(factory.contextBoundOnCreate.get(), Boolean.FALSE, "the creator thread must inherit the triggering thread's (absent) context, not the starting thread's");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.FALSE, "the triggering thread's scope is not inherited by the creator thread");
    } finally {
      pool.shutdown();
    }
  }

  // The scope is resolved once per start, so a scope set while the pool is started binds neither the running
  // deconstruction timer nor the creator threads triggered meanwhile; it is seen by the next start.
  @Test
  public void testScopeSetAfterStartupIsNotSeenUntilTheNextStart ()
    throws ComponentPoolException, InterruptedException {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("late-scope", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));

    Assert.assertFalse(PerApplicationScope.isBound(), "the starting thread must carry no scope");
    pool.startup();
    try {
      pool.setPerApplicationScope(perApplicationScope);

      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.FALSE, "a creator thread triggered after the scope was set still runs with the scope resolved at startup");

      pool.returnInstance(factory.instance.get());
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.scopeBoundOnClose.get(), Boolean.FALSE, "the deconstruction thread, started before the scope was set, is not rebound");
    } finally {
      pool.shutdown();
    }

    pool.startup();
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "the next start resolves the scope set in between");
    } finally {
      pool.shutdown();
    }
  }

  // A triggering thread that carries a context hands it on to the creator thread, while the scope, given to neither
  // the pool nor the triggering thread, stays absent.
  @Test
  public void testCreatorThreadInheritsAContextPresentOnTheTriggeringThread ()
    throws Exception {

    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("creator-inherits-context", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    pool.startup();
    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

        try {
          new PerApplicationContext();
          Assert.assertEquals(pool.getComponent(), "component");
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      });

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(factory.contextBoundOnCreate.get(), Boolean.TRUE, "the creator thread must inherit the context present on the triggering thread");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.FALSE, "no scope was given to the pool or bound on the triggering thread");
    } finally {
      pool.shutdown();
    }
  }

  // With no scope given, the pool adopts the one bound on the thread that starts it, so the deconstruction thread
  // runs inside that scope whether or not a context is attached as well.
  @Test
  public void testStartedInsideAScopeItWasNotGivenAdoptsThatScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("adopted", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

      try {
        Assert.assertFalse(PerApplicationContext.isBound(), "the starting thread must carry no context");
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();
    rethrow(failure);

    try {
      Assert.assertNull(pool.getPerApplicationScope(), "adoption does not alter the handed value; it shows on the pool's threads");

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.seenOnClose.get(), "scoped", "the deconstruction thread must run inside the adopted scope");
    } finally {
      pool.shutdown();
    }
  }

  @Test
  public void testStartedInsideAScopeAdoptsItEvenWithAContextAttached ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("adopted-with-context", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

      try {
        new PerApplicationContext();
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();
    rethrow(failure);

    try {
      Assert.assertNull(pool.getPerApplicationScope(), "a context on the starting thread does not prevent adoption, and adoption does not alter the handed value");

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.seenOnClose.get(), "scoped", "the deconstruction thread must run inside the adopted scope");
    } finally {
      pool.shutdown();
    }
  }

  // The adopted scope is kept for the pool's own threads, so a creator thread triggered later, from a thread bound
  // to nothing, runs inside it as well.
  @Test
  public void testCreatorThreadRunsInsideTheAdoptedScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("adopted-creator", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

      try {
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();
    rethrow(failure);

    try {
      Assert.assertFalse(PerApplicationScope.isBound(), "the triggering thread must carry no scope");
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "a creator thread must run inside the scope the pool adopted at startup");
    } finally {
      pool.shutdown();
    }
  }

  @Test
  public void testGivenTheScopeItIsStartedInsideRunsInsideIt ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("scope-given", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS)).setPerApplicationScope(perApplicationScope);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

      try {
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();
    rethrow(failure);

    try {
      Assert.assertSame(pool.getPerApplicationScope(), perApplicationScope);

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.seenOnClose.get(), "scoped");
    } finally {
      pool.shutdown();
    }
  }

  // A scope given to the pool that is not the one bound on the starting thread could never see the registrations
  // made under the bound scope, so startup fails and leaves the pool stopped, still holding the scope it was given.
  @Test
  public void testGivenADifferentScopeThanTheOneBoundFailsAtStartup ()
    throws Exception {

    PerApplicationScope handedScope = new PerApplicationScope();
    ComponentPool<String> pool = new ComponentPool<>("scope-conflict", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)).setPerApplicationScope(handedScope);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

      try {
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    }));

    thread.start();
    thread.join();

    Assert.assertTrue(failure.get() instanceof ScopeException, "expected a ScopeException, saw " + failure.get());
    Assert.assertSame(pool.getPerApplicationScope(), handedScope, "a failed resolution must not alter the scope the pool was given");

    try {
      pool.getComponent();
      Assert.fail("the pool must not be started after a failed resolution");
    } catch (ComponentPoolException componentPoolException) {
      Assert.assertTrue(componentPoolException.getMessage().contains("not in the 'started' state"), componentPoolException.getMessage());
    }
  }

  @Test
  public void testStartedWithNeitherBindingRunsUnbound ()
    throws Exception {

    RecordingFactory factory = new RecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("neither-binding", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

      try {
        Assert.assertFalse(PerApplicationScope.isBound(), "the starting thread must carry no scope");
        Assert.assertFalse(PerApplicationContext.isBound(), "the starting thread must carry no context");
        pool.startup();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();
    rethrow(failure);

    try {
      Assert.assertNull(pool.getPerApplicationScope(), "nothing given and nothing bound resolves to no scope");

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertTrue(factory.seenOnClose.get() instanceof MissingPerApplicationScopeException, "expected an unbound reaper, saw " + factory.seenOnClose.get());
    } finally {
      pool.shutdown();
    }
  }

  // Resolution happens inside the pin manager's start guard, so a second startup() on a running pool is a no-op that
  // neither adopts the scope bound on its own thread nor rejects the one the pool already holds.
  @Test
  public void testSecondStartupOnAStartedPoolDoesNotResolveAgain ()
    throws Exception {

    PerApplicationScope handedScope = new PerApplicationScope();
    ComponentPool<String> pool = new ComponentPool<>("resolved-once", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)).setPerApplicationScope(handedScope);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Assert.assertFalse(PerApplicationScope.isBound(), "the starting thread must carry no scope");
    pool.startup();
    try {
      Assert.assertSame(pool.getPerApplicationScope(), handedScope);

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

        try {
          pool.startup();
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertSame(pool.getPerApplicationScope(), handedScope, "a second startup on a started pool must not resolve again");
    } finally {
      pool.shutdown();
    }
  }

  // Resolution happens on every STOPPED to STARTING transition, on the starting thread: a pool first started on an
  // unbound thread runs with no scope, and the same pool shut down and started again inside a scope adopts it.
  @Test
  public void testStartAfterShutdownResolvesAgain ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("resolved-again", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));

    Assert.assertFalse(PerApplicationScope.isBound(), "the first starting thread must carry no scope");
    pool.startup();
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.FALSE, "the first start resolves to no scope");
    } finally {
      pool.shutdown();
    }

    startInside(perApplicationScope, pool);
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "a start after a shutdown must resolve again, on its own thread");
      Assert.assertNull(pool.getPerApplicationScope(), "adoption does not alter the handed value");
    } finally {
      pool.shutdown();
    }
  }

  // Each start resolves the handed value afresh on its own thread, so a scope adopted at one start is not carried
  // into the next: a start on an unbound thread runs with no scope, and a start inside another scope adopts that one
  // rather than rejecting it.
  @Test
  public void testScopeAdoptedAtOneStartIsNotCarriedIntoTheNext ()
    throws Exception {

    PerApplicationScope firstScope = new PerApplicationScope();
    PerApplicationScope secondScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("not-carried-over", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));

    startInside(firstScope, pool);
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "the first start adopts the bound scope");
    } finally {
      pool.shutdown();
    }

    Assert.assertFalse(PerApplicationScope.isBound(), "the second starting thread must carry no scope");
    pool.startup();
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.FALSE, "a start on an unbound thread does not keep the scope adopted earlier");
    } finally {
      pool.shutdown();
    }

    startInside(secondScope, pool);
    try {
      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "a start inside another scope adopts that scope");
    } finally {
      pool.shutdown();
    }
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  // Records what the creator thread and the reaping thread see of either binding, and keeps the instance so the
  // test can return it to the pool.
  private static class BindingRecordingFactory extends AbstractComponentInstanceFactory<String> {

    private final AtomicReference<ComponentInstance<String>> instance = new AtomicReference<>();
    private final AtomicReference<Boolean> contextBoundOnCreate = new AtomicReference<>();
    private final AtomicReference<Boolean> scopeBoundOnCreate = new AtomicReference<>();
    private final AtomicReference<Boolean> scopeBoundOnClose = new AtomicReference<>();

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      ComponentInstance<String> componentInstance;

      contextBoundOnCreate.set(PerApplicationContext.isBound());
      scopeBoundOnCreate.set(PerApplicationScope.isBound());

      instance.set(componentInstance = new ComponentInstance<>() {

        @Override
        public boolean validate () {

          return true;
        }

        @Override
        public String serve () {

          return "component";
        }

        @Override
        public void close () {

          scopeBoundOnClose.set(PerApplicationScope.isBound());
        }

        @Override
        public StackTraceElement[] getExistentialStackTrace () {

          return null;
        }
      });

      return componentInstance;
    }
  }

  private static class RegistryRecordingFactory extends AbstractComponentInstanceFactory<String> {

    private final AtomicReference<Object> seenOnClose = new AtomicReference<>();

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      return new ComponentInstance<>() {

        @Override
        public boolean validate () {

          return true;
        }

        @Override
        public String serve () {

          return "component";
        }

        @Override
        public void close () {

          seenOnClose.set(ClaxonRegistryManager.getRegistry());
        }

        @Override
        public StackTraceElement[] getExistentialStackTrace () {

          return null;
        }
      };
    }
  }

  private static class RecordingFactory extends AbstractComponentInstanceFactory<String> {

    private final AtomicReference<Object> seenOnClose = new AtomicReference<>();

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      return new ComponentInstance<>() {

        @Override
        public boolean validate () {

          return true;
        }

        @Override
        public String serve () {

          return "component";
        }

        @Override
        public void close () {

          try {
            seenOnClose.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
          } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
            seenOnClose.set(missingPerApplicationScopeException);
          }
        }

        @Override
        public StackTraceElement[] getExistentialStackTrace () {

          return null;
        }
      };
    }
  }
}
