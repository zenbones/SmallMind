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
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Proves that a pool given a {@link PerApplicationScope} reaps components on a deconstruction thread bound to that
 * scope, and that a pool given none reaps them unbound. The component records what its own thread sees when the
 * pool closes it, so the assertion is on the reaping thread's binding rather than the test thread's.
 */
@Test(groups = "unit")
public class ComponentPoolScopeTest {

  private static final int LIMIT_SECONDS = 1;
  private static final long SETTLE_MILLIS = 2500L;
  private static final long CREATION_TIMEOUT_MILLIS = 5000L;

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

  // The deconstruction timer is bound when the pool starts, but each creator thread reads the scope when its
  // creation is triggered, so a scope set after startup() binds the creator threads and not the reaper.
  @Test
  public void testScopeSetAfterStartupBindsCreatorThreadsButNotTheDeconstructionThread ()
    throws ComponentPoolException, InterruptedException {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("late-scope", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));

    pool.startup();
    try {
      pool.setPerApplicationScope(perApplicationScope);

      Assert.assertEquals(pool.getComponent(), "component");
      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "a creator thread started after the scope was set must run inside it");

      pool.returnInstance(factory.instance.get());
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the idle component should have been reaped");
      Assert.assertEquals(factory.scopeBoundOnClose.get(), Boolean.FALSE, "the deconstruction thread, started before the scope was set, is not rebound");
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

  // The scope is read when a creation is triggered, from whichever thread triggers it, so one set from another thread
  // after startup() is what a later creator thread runs inside.
  @Test
  public void testScopeSetFromOneThreadBindsACreatorTriggeredFromAnother ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    BindingRecordingFactory factory = new BindingRecordingFactory();
    ComponentPool<String> pool = new ComponentPool<>("late-scope-across-threads", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setCreationTimeoutMillis(CREATION_TIMEOUT_MILLIS));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    pool.startup();
    try {

      Thread settingThread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> pool.setPerApplicationScope(perApplicationScope));
      Thread triggeringThread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

        try {
          Assert.assertEquals(pool.getComponent(), "component");
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      });

      settingThread.start();
      settingThread.join();
      triggeringThread.start();
      triggeringThread.join();
      rethrow(failure);

      Assert.assertEquals(factory.scopeBoundOnCreate.get(), Boolean.TRUE, "a creator thread triggered after the scope was set from another thread must run inside it");
    } finally {
      pool.shutdown();
    }
  }

  // The warning covers the one gap the pool can not close itself: started inside a scope it was not handed, with no
  // context for its deconstruction and creator threads to inherit either.
  @Test
  public void testStartupWarnsWhenStartedInsideAScopeItWasNotGivenWithNoContextAttached ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

        try {
          startAndStop(new ComponentPool<>("warned", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)));
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 1, "exactly one warning is expected, saw " + appender.getRecords());
      Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.WARN);
      Assert.assertTrue(appender.getRecords().get(0).getMessage().contains("without being given that scope"), appender.getRecords().get(0).getMessage());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  @Test
  public void testStartupDoesNotWarnWhenAContextIsAttachedToTheStartingThread ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

        try {
          new PerApplicationContext();
          startAndStop(new ComponentPool<>("context-attached", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)));
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 0, "no warning is expected when the pool's threads can inherit a context, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  @Test
  public void testStartupDoesNotWarnWhenGivenTheScopeItIsStartedInside ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    PerApplicationScope perApplicationScope = new PerApplicationScope();
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> perApplicationScope.run(() -> {

        try {
          startAndStop(new ComponentPool<>("scope-given", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)).setPerApplicationScope(perApplicationScope));
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 0, "no warning is expected when the pool was given the scope, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  @Test
  public void testStartupDoesNotWarnWhenTheStartingThreadCarriesNeitherBinding ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {

        try {
          Assert.assertFalse(PerApplicationScope.isBound(), "the starting thread must carry no scope");
          Assert.assertFalse(PerApplicationContext.isBound(), "the starting thread must carry no context");
          startAndStop(new ComponentPool<>("neither-binding", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0)));
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      });

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 0, "no warning is expected when the starting thread carries neither binding, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  // The advisory fires inside the pin manager's start guard, so a second startup() on a running pool is a no-op that
  // logs nothing further.
  @Test
  public void testSecondStartupOnAStartedPoolDoesNotWarnAgain ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

        ComponentPool<String> pool = new ComponentPool<>("warned-once", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0));

        try {
          pool.startup();
          try {
            Assert.assertEquals(appender.size(), 1, "the first startup is expected to warn exactly once, saw " + appender.getRecords());
            pool.startup();
          } finally {
            pool.shutdown();
          }
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 1, "a second startup on a started pool must not warn again, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  // The advisory fires on every STOPPED to STARTING transition, so a pool shut down and started again on the same
  // scope-bound, context-less thread warns a second time.
  @Test
  public void testStartAfterShutdownWarnsAgain ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(ComponentPool.class, Level.WARN);
    AtomicReference<Throwable> failure = new AtomicReference<>();

    try {

      Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> new PerApplicationScope().run(() -> {

        ComponentPool<String> pool = new ComponentPool<>("warned-again", new RecordingFactory(), new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0));

        try {
          startAndStop(pool);
          Assert.assertEquals(appender.size(), 1, "the first startup is expected to warn exactly once, saw " + appender.getRecords());
          startAndStop(pool);
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }));

      thread.start();
      thread.join();
      rethrow(failure);

      Assert.assertEquals(appender.size(), 2, "a startup after a shutdown must warn again, saw " + appender.getRecords());
      Assert.assertEquals(appender.getRecords().get(1).getLevel(), Level.WARN);
      Assert.assertTrue(appender.getRecords().get(1).getMessage().contains("without being given that scope"), appender.getRecords().get(1).getMessage());
    } finally {
      CapturingAppender.release(ComponentPool.class);
    }
  }

  private static void startAndStop (ComponentPool<String> pool)
    throws ComponentPoolException {

    pool.startup();
    pool.shutdown();
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
