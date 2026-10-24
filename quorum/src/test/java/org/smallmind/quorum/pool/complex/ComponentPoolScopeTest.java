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
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.quorum.pool.ComponentPoolException;
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

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation still resolves through the context flavor; keep it satisfied as the
    // other pool tests do, so that only the scope flavor is under test here.
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

  public static class SampleManager implements PerApplicationDataManager {

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
