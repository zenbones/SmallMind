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
package org.smallmind.spark.tanukisoft.integration.spring;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.property.PropertyExpanderException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.spring.PropertyPlaceholderStringValueResolver;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorScopeManager;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.tanukisoftware.wrapper.WrapperManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

@org.testng.annotations.Test(groups = "unit")
public class SpringContextWrapperListenerTest {

  private static SpringPropertyAccessor emptyAccessor () {

    try {

      return new SpringPropertyAccessor(new PropertyPlaceholderStringValueResolver(new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false), new HashMap<>()));
    } catch (PropertyExpanderException propertyExpanderException) {
      throw new RuntimeException(propertyExpanderException);
    }
  }

  // Every test but the explicit "scope off" one runs with spark.per.application.scope set, as a wrapper.conf built
  // with the plugin's perApplicationScope parameter would have it.
  @BeforeMethod
  public void resetWrapperManager () {

    WrapperManager.reset();
    WrapperManager.getProperties().setProperty("spark.per.application.scope", "true");
  }

  // The listener's own scope is published as a singleton during the refresh, by a post-processor the listener adds, so
  // bean definitions can ref it.
  public void testStartupRegistersTheListenersScopeAsASingleton () {

    CountingListener listener = new CountingListener();

    listener.startup(new String[0]);

    Assert.assertSame(listener.context.getBean(SpringContextWrapperListener.PER_APPLICATION_SCOPE_BEAN_NAME, PerApplicationScope.class), listener.exposedScope());
  }

  // Without spark.per.application.scope the listener has no scope: the context is loaded and refreshed with only the
  // context binding, no scope singleton is registered, and shutdown closes it the same way.
  public void testStartupWithoutTheScopeRegistersNoSingletonAndLoadsUnbound () {

    BindingRecordingListener listener = new BindingRecordingListener();

    WrapperManager.getProperties().remove("spark.per.application.scope");
    listener.startup(new String[0]);

    Assert.assertEquals(listener.scopeBoundOnLoad, Boolean.FALSE, "no scope was asked for");
    Assert.assertTrue(listener.context.isActive());
    Assert.assertFalse(listener.context.containsBean(SpringContextWrapperListener.PER_APPLICATION_SCOPE_BEAN_NAME));

    listener.shutdown();

    Assert.assertFalse(listener.context.isActive());
  }

  // A context handed back already refreshed is left alone; no post-processor is added for an active context.
  public void testStartupLeavesAnAlreadyActiveContextWithoutTheScopeBean () {

    CountingListener listener = new CountingListener(true);

    listener.startup(new String[0]);

    Assert.assertFalse(listener.context.containsBean(SpringContextWrapperListener.PER_APPLICATION_SCOPE_BEAN_NAME));
  }

  public void testStartupCreatesAndRefreshesTheContext () {

    CountingListener listener = new CountingListener();

    listener.startup(new String[0]);

    Assert.assertEquals(listener.loadCount, 1);
  }

  public void testStartupIsIdempotentForAnAlreadyActiveContext () {

    CountingListener listener = new CountingListener();

    listener.startup(new String[0]);
    listener.startup(new String[0]);

    // The context is created once and not refreshed a second time while it remains active.
    Assert.assertEquals(listener.loadCount, 1);
  }

  public void testShutdownClosesAnActiveContext ()
    throws Exception {

    CountingListener listener = new CountingListener();

    listener.startup(new String[0]);
    listener.shutdown();

    Assert.assertFalse(listener.context.isActive(), "shutdown() must close the context it started");

    // A clean shutdown followed by a second shutdown must not fail, even though the context is no longer active.
    listener.shutdown();
  }

  public void testShutdownWithoutStartupIsHarmless ()
    throws Exception {

    new CountingListener().shutdown();
  }

  // startup() refreshes inside the listener's own scope even when the caller is not already inside it, so a
  // registration made through a bridging manager during the refresh lands in the scope.
  public void testDirectStartupRefreshesInsideTheListenersScope () {

    RegisteringListener listener = new RegisteringListener();

    listener.startup(new String[0]);
    try {
      Assert.assertSame(listener.exposedScope().call(SpringPropertyAccessorManager::getSpringPropertyAccessor), listener.accessor);
      Assert.assertSame(listener.exposedScope().call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor), listener.accessor, "the scope map itself must hold the registration, not only the context the listener attached to this thread");
    } finally {
      listener.shutdown();
    }
  }

  // A caller already inside the listener's scope simply re-enters it (harmless), and the refresh still registers
  // through that scope.
  public void testDirectStartupInsideTheListenersScopeRegistersThroughIt () {

    RegisteringListener listener = new RegisteringListener();

    listener.exposedScope().run(() -> listener.startup(new String[0]));
    try {
      Assert.assertSame(listener.exposedScope().call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor), listener.accessor);
    } finally {
      listener.shutdown();
    }
  }

  // start() runs the whole startup inside the listener's scope, and this thread carries the listener's context map
  // (attached by it, or already present), so a bean registering during the refresh lands in both. A plain thread
  // started here inherits only the context, and still reads the registration back through it.
  public void testRegistrationMadeDuringStartIsReadBackByAnUnwrappedThreadThroughTheInheritedContext ()
    throws Exception {

    BeanRegisteringListener listener = new BeanRegisteringListener();
    AtomicReference<Object> seenOnThread = new AtomicReference<>();
    AtomicReference<Boolean> scopeBoundOnThread = new AtomicReference<>();

    Assert.assertNull(listener.start(new String[0]));
    try {

      Thread thread = new Thread(() -> {
        scopeBoundOnThread.set(PerApplicationScope.isBound());
        seenOnThread.set(SpringPropertyAccessorManager.getSpringPropertyAccessor());
      }, "spark-test-reader");

      thread.setDaemon(false);
      thread.start();
      thread.join();

      Assert.assertEquals(scopeBoundOnThread.get(), Boolean.FALSE, "a plain thread inherits no scope");
      Assert.assertSame(seenOnThread.get(), listener.accessor, "the registration must be reachable through the context the thread inherited");
    } finally {
      listener.shutdown();
    }
  }

  // loadApplicationContext() is called inside the listener's scope as well as refresh(), so anything a listener does
  // while building the context, before the refresh, is already under the scope.
  public void testDirectStartupLoadsTheContextInsideTheListenersScope () {

    BindingRecordingListener listener = new BindingRecordingListener();

    listener.startup(new String[0]);
    try {
      Assert.assertEquals(listener.scopeBoundOnLoad, Boolean.TRUE, "loadApplicationContext() must run inside the listener's scope");
    } finally {
      listener.shutdown();
    }
  }

  // startup() binds the listener's own scope even over a caller already inside some other scope, so the registration
  // lands in the listener's scope and the caller's scope is left empty.
  public void testDirectStartupFromAThreadBoundToADifferentScopeRegistersIntoTheListenersScope () {

    RegisteringListener listener = new RegisteringListener();
    PerApplicationScope callerScope = new PerApplicationScope();

    callerScope.run(() -> listener.startup(new String[0]));
    try {
      Assert.assertSame(listener.exposedScope().call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor), listener.accessor, "the registration must land in the listener's scope");
      Assert.assertNull(callerScope.call(SpringPropertyAccessorScopeManager::getSpringPropertyAccessor), "the caller's scope must not receive the registration");
    } finally {
      listener.shutdown();
    }
  }

  // A failure inside loadApplicationContext() leaves the listener without a context, so the next startup() loads
  // again rather than refreshing nothing.
  public void testStartupLoadsTheContextAgainAfterAFailedFirstLoad () {

    FailOnceListener listener = new FailOnceListener();

    try {
      listener.startup(new String[0]);
      Assert.fail("the first load is expected to fail");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertEquals(illegalStateException.getMessage(), "first load fails");
    }

    listener.startup(new String[0]);
    try {
      Assert.assertEquals(listener.loadCount, 2, "the second startup must load the context again");
      Assert.assertNotNull(listener.context, "the second startup must create the context");
      Assert.assertTrue(listener.context.isActive(), "the context created on the retry must be refreshed");
    } finally {
      listener.shutdown();
    }
  }

  // The documented caveat: the same context instance is kept across stop() and refreshed again on the next startup(),
  // which a GenericApplicationContext refuses.
  public void testStartupAfterStopRefusesToRefreshAGenericApplicationContextAgain () {

    CountingListener listener = new CountingListener();

    Assert.assertNull(listener.start(new String[0]));
    Assert.assertEquals(listener.stop(0), 0);
    Assert.assertFalse(listener.context.isActive(), "stop() must have closed the context");

    try {
      listener.startup(new String[0]);
      Assert.fail("a GenericApplicationContext can not be refreshed a second time");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertEquals(listener.loadCount, 1, "the listener must keep the same context instance rather than loading a new one");
    }
  }

  // shutdown() closes inside the listener's scope whether reached through stop() or called directly, so a bean's
  // destroy callback runs bound to that scope.
  public void testDirectShutdownDestroysBeansInsideTheListenersScope () {

    DestroyRecordingListener listener = new DestroyRecordingListener();

    listener.startup(new String[0]);

    Assert.assertFalse(PerApplicationScope.isBound(), "the test thread must not be bound for this case to mean anything");

    listener.shutdown();

    Assert.assertEquals(listener.scopeBoundOnDestroy.get(), Boolean.TRUE, "destroy must run inside the listener's scope");
  }

  // Registers through the bridging manager from a bean factory post-processor, which runs inside refresh().
  private static class RegisteringListener extends SpringContextWrapperListener {

    private final SpringPropertyAccessor accessor = emptyAccessor();

    private PerApplicationScope exposedScope () {

      return getPerApplicationScope();
    }

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      GenericApplicationContext context = new GenericApplicationContext();

      context.addBeanFactoryPostProcessor((beanFactory) -> SpringPropertyAccessorManager.register(accessor));

      return context;
    }
  }

  // Registers through the bridging manager from a bean's afterPropertiesSet(), which runs inside refresh().
  private static class BeanRegisteringListener extends SpringContextWrapperListener {

    private final SpringPropertyAccessor accessor = emptyAccessor();

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      GenericApplicationContext context = new GenericApplicationContext();

      context.registerBean("registeringBean", RegisteringBean.class, () -> new RegisteringBean(accessor));

      return context;
    }
  }

  private static class RegisteringBean implements InitializingBean {

    private final SpringPropertyAccessor accessor;

    private RegisteringBean (SpringPropertyAccessor accessor) {

      this.accessor = accessor;
    }

    @Override
    public void afterPropertiesSet () {

      SpringPropertyAccessorManager.register(accessor);
    }
  }

  // Records whether the listener's scope was bound on the thread when the context was loaded.
  private static class BindingRecordingListener extends SpringContextWrapperListener {

    private GenericApplicationContext context;
    private Boolean scopeBoundOnLoad;

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      scopeBoundOnLoad = PerApplicationScope.isBound();

      return context = new GenericApplicationContext();
    }
  }

  // Fails the first load and succeeds afterwards, so the listener's retry path can be observed.
  private static class FailOnceListener extends SpringContextWrapperListener {

    private GenericApplicationContext context;
    private int loadCount = 0;

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      if (++loadCount == 1) {
        throw new IllegalStateException("first load fails");
      }

      context = new GenericApplicationContext();

      return context;
    }
  }

  // Registers a bean that records, when destroyed, whether the thread is bound to this listener's scope.
  private static class DestroyRecordingListener extends SpringContextWrapperListener {

    private final AtomicReference<Boolean> scopeBoundOnDestroy = new AtomicReference<>();

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      GenericApplicationContext context = new GenericApplicationContext();
      PerApplicationScope perApplicationScope = getPerApplicationScope();

      context.registerBean("destroyRecordingBean", DestroyRecordingBean.class, () -> new DestroyRecordingBean(perApplicationScope, scopeBoundOnDestroy));

      return context;
    }
  }

  private static class DestroyRecordingBean implements DisposableBean {

    private final PerApplicationScope perApplicationScope;
    private final AtomicReference<Boolean> scopeBoundOnDestroy;

    private DestroyRecordingBean (PerApplicationScope perApplicationScope, AtomicReference<Boolean> scopeBoundOnDestroy) {

      this.perApplicationScope = perApplicationScope;
      this.scopeBoundOnDestroy = scopeBoundOnDestroy;
    }

    @Override
    public void destroy () {

      scopeBoundOnDestroy.set(PerApplicationScope.isBound() && perApplicationScope.isBoundOnCurrentThread());
    }
  }

  private static class CountingListener extends SpringContextWrapperListener {

    private final boolean preRefreshed;
    private GenericApplicationContext context;
    private int loadCount = 0;

    private CountingListener () {

      this(false);
    }

    private CountingListener (boolean preRefreshed) {

      this.preRefreshed = preRefreshed;
    }

    private PerApplicationScope exposedScope () {

      return getPerApplicationScope();
    }

    @Override
    public ConfigurableApplicationContext loadApplicationContext (String[] args) {

      loadCount++;
      context = new GenericApplicationContext();

      if (preRefreshed) {
        context.refresh();
      }

      return context;
    }
  }
}
