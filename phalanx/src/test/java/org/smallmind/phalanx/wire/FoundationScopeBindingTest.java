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
package org.smallmind.phalanx.wire;

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorContextManager;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorScopeManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Boots the testbench foundation context the way TESTBENCH.adoc describes, with the refresh inside a test-owned
 * {@link PerApplicationScope} and every test method bound through {@link IHookable}, on a thread that carries no
 * {@link PerApplicationContext} beforehand, and records where the accessor registered by the foundation's
 * {@code PropertyPlaceholderConfigurer} landed: in the scope map, and in the context map the foundation's
 * {@code perApplicationContext} bean attaches to the refresh thread.
 */
// @Test is applied per method rather than on the class: a class-level @Test would make the public IHookable.run
// a test method of its own, which TestNG cannot inject parameters for. Without the class-level annotation the
// configuration methods no longer inherit its group, so they carry it explicitly or a group-filtered run skips them.
public class FoundationScopeBindingTest implements IHookable {

  private static final PerApplicationScope SCOPE = new PerApplicationScope();
  private static final AtomicReference<Boolean> CONTEXT_BOUND_AFTER_REFRESH = new AtomicReference<>();
  private static final AtomicReference<SpringPropertyAccessor> CONTEXT_MAP_ACCESSOR = new AtomicReference<>();
  private static final AtomicReference<MissingPerApplicationContextException> CONTEXT_MAP_FAILURE = new AtomicReference<>();
  private static ConfigurableApplicationContext context;

  @BeforeClass(groups = "unit")
  public void beforeClass ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        Assert.assertFalse(PerApplicationContext.isBound(), "the refresh thread must start without a context for the observation to mean anything");

        SCOPE.run(() -> {
          context = new ClassPathXmlApplicationContext(new String[] {"org/smallmind/testbench/foundation/foundation.xml"}, false);
          context.addBeanFactoryPostProcessor((beanFactory) -> beanFactory.registerSingleton("org.smallmind.spark.perApplicationScope", SCOPE));
          context.refresh();
        });

        // outside the scope now, on the thread the foundation's PerApplicationContext bean attached its map to
        CONTEXT_BOUND_AFTER_REFRESH.set(PerApplicationContext.isBound());
        try {
          CONTEXT_MAP_ACCESSOR.set(SpringPropertyAccessorContextManager.getSpringPropertyAccessor());
        } catch (MissingPerApplicationContextException missingPerApplicationContextException) {
          CONTEXT_MAP_FAILURE.set(missingPerApplicationContextException);
        }
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
  }

  @AfterClass(groups = "unit")
  public void afterClass () {

    if (context != null) {
      SCOPE.run(context::close);
    }
  }

  @Override
  public void run (IHookCallBack callBack, ITestResult testResult) {

    SCOPE.run(() -> callBack.runTestMethod(testResult));
  }

  @Test(groups = "unit")
  public void testHookBindsTheScopeAroundEveryTestMethod () {

    Assert.assertTrue(PerApplicationScope.isBound());
    Assert.assertTrue(SCOPE.isBoundOnCurrentThread());
  }

  @Test(groups = "unit")
  public void testFoundationAccessorLandsInTheScopeMap () {

    Assert.assertNotNull(SCOPE.call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor));
    Assert.assertSame(SpringPropertyAccessorScopeManager.getSpringPropertyAccessor(), SCOPE.call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor), "the hook binds the same scope the refresh ran in");
  }

  // Spring instantiates every PriorityOrdered BeanFactoryPostProcessor before invoking any of them, and getBean on the
  // foundation's systemProperties follows its depends-on chain (securityProviders -> perApplicationContext). The
  // PerApplicationContext bean has therefore already attached a map to the refresh thread by the time allProperties
  // runs postProcessBeanFactory, so SpringPropertyAccessorManager.register writes the accessor into both maps.
  @Test(groups = "unit")
  public void testFoundationAccessorAlsoLandsInTheContextMapAttachedByTheRefresh () {

    Assert.assertNull(CONTEXT_MAP_FAILURE.get(), "the refresh thread carried no context map after the refresh");
    Assert.assertEquals(CONTEXT_BOUND_AFTER_REFRESH.get(), Boolean.TRUE);
    Assert.assertNotNull(CONTEXT_MAP_ACCESSOR.get());
    Assert.assertSame(CONTEXT_MAP_ACCESSOR.get(), SCOPE.call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor));
  }
}
