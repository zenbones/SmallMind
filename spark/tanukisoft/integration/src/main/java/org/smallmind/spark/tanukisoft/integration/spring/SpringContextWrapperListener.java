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

import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.spark.tanukisoft.integration.AbstractWrapperListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link AbstractWrapperListener} specialization that maps the wrapper's start/stop lifecycle onto a Spring
 * {@link ConfigurableApplicationContext}: startup loads and refreshes the context, shutdown closes it. Concrete
 * subclasses decide how the context is produced.
 * <p>
 * Loading, refreshing, and closing all run inside the listener's {@code PerApplicationScope} (see
 * {@link AbstractWrapperListener#getPerApplicationScope()}), and that same instance is registered as the singleton
 * bean named {@value #PER_APPLICATION_SCOPE_BEAN_NAME} during the refresh — by a {@code BeanFactoryPostProcessor}
 * this listener adds when the context handed back by {@link #loadApplicationContext(String[])} is not yet active —
 * so bean definitions can {@code ref} it (for example into a web server's {@code perApplicationScope} property). A
 * bean-defined {@code BeanDefinitionRegistryPostProcessor} runs before that and cannot see it. A context that
 * arrives already refreshed receives no such bean.
 */
public abstract class SpringContextWrapperListener extends AbstractWrapperListener {

  /**
   * Name of the singleton under which the listener's {@code PerApplicationScope} is registered during each refresh.
   */
  public static final String PER_APPLICATION_SCOPE_BEAN_NAME = "org.smallmind.spark.perApplicationScope";

  private ConfigurableApplicationContext applicationContext;

  /**
   * Factory hook implemented by subclasses to supply the Spring context that the listener should manage.
   *
   * @param args the wrapper-supplied arguments (after timeout stripping); normally interpreted as config locations
   * @return a newly created context, which should be unrefreshed so that the scope singleton can be registered
   */
  public abstract ConfigurableApplicationContext loadApplicationContext (String[] args);

  /**
   * Runs entirely inside the listener's {@code PerApplicationScope}: lazily creates the application context on first
   * start, adds (once per context instance) a post-processor that registers the scope as the
   * {@value #PER_APPLICATION_SCOPE_BEAN_NAME} singleton on every refresh of that context, and refreshes it if it is
   * not active, so Spring beans are available and every registration made while loading or refreshing lands in the
   * scope (and in the inherited {@code PerApplicationContext}). A singleton already registered under that name by
   * something else makes the refresh fail with Spring's {@code IllegalStateException} rather than being kept quietly.
   * <p>
   * Reached through {@link #start(String[])} the calling thread is already inside the scope; invoked directly, this
   * method binds it itself, so both paths behave identically. A second {@code startup} after {@link #shutdown()} in
   * the same JVM refreshes the same context again with the scope bean present, provided the context supports being
   * refreshed more than once ({@code ClassPathXmlApplicationContext} does; {@code GenericApplicationContext} and its
   * subclasses throw {@code IllegalStateException} on a second refresh). The context returned by
   * {@link #loadApplicationContext(String[])} must be unrefreshed; one that arrives active is not refreshed on this
   * call and never receives the scope bean, even if a later call refreshes it again.
   *
   * @param args arguments forwarded from the wrapper, passed through to {@link #loadApplicationContext(String[])}
   */
  @Override
  public void startup (String[] args) {

    PerApplicationScope perApplicationScope = getPerApplicationScope();

    perApplicationScope.run(() -> {
      if (applicationContext == null) {
        if (!(applicationContext = loadApplicationContext(args)).isActive()) {
          applicationContext.addBeanFactoryPostProcessor((beanFactory) -> beanFactory.registerSingleton(PER_APPLICATION_SCOPE_BEAN_NAME, perApplicationScope));
        }
      }
      if (!applicationContext.isActive()) {
        applicationContext.refresh();
      }
    });
  }

  /**
   * Closes the managed application context if one has been created and is still active, inside the listener's
   * {@code PerApplicationScope} so that bean destruction callbacks resolve the same registrations the refresh made
   * whether this method is reached through {@link #stop(int)} or called directly.
   */
  @Override
  public void shutdown () {

    getPerApplicationScope().run(() -> {
      if ((applicationContext != null) && applicationContext.isActive()) {
        applicationContext.close();
      }
    });
  }
}
