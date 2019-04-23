/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.web.jersey.spring;

import java.util.Set;
import java.util.function.Supplier;
import javax.servlet.ServletContext;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

public class SpringHK2ComponentProvider implements ComponentProvider {

  private volatile InjectionManager injectionManager;
  private volatile ApplicationContext applicationContext;

  @Override
  public void initialize (InjectionManager injectionManager) {

    this.injectionManager = injectionManager;

    ServletContext sc = injectionManager.getInstance(ServletContext.class);

    LoggerManager.getLogger(SpringHK2ComponentProvider.class).info("Searching for Spring application context on Thread(%s)", Thread.currentThread().getName());
    if ((applicationContext = ExposedApplicationContext.getApplicationContext()) == null) {
      throw new SpringHK2IntegrationException("Spring application context has not been created prior to HK2 application initialization");
    }

    // initialize HK2 spring-bridge
    ImmediateHk2InjectionManager hk2InjectionManager = (ImmediateHk2InjectionManager)injectionManager;
    SpringBridge.getSpringBridge().initializeSpringBridge(hk2InjectionManager.getServiceLocator());
    SpringIntoHK2Bridge springBridge = injectionManager.getInstance(SpringIntoHK2Bridge.class);
    springBridge.bridgeSpringBeanFactory(applicationContext);

    injectionManager.register(Bindings.injectionResolver(new AutowiredInjectResolver(applicationContext)));
    injectionManager.register(Bindings.service(applicationContext).to(ApplicationContext.class).named("SpringContext"));
  }

  // detect JAX-RS classes that are also Spring @Components.
  // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
  @Override
  public boolean bind (Class<?> component, Set<Class<?>> providerContracts) {

    if (applicationContext == null) {
      return false;
    }

    if (AnnotationUtils.findAnnotation(component, Component.class) == null) {

      return false;
    } else {

      String[] beanNames = applicationContext.getBeanNamesForType(component);

      if (beanNames == null || beanNames.length != 1) {

        return false;
      } else {

        String beanName = beanNames[0];

        Binding binding = Bindings.supplier(new SpringManagedBeanFactory(applicationContext, injectionManager, beanName)).to(component).to(providerContracts);
        injectionManager.register(binding);

        return true;
      }
    }
  }

  @Override
  public void done () {

  }

  private static class SpringManagedBeanFactory implements Supplier {

    private final ApplicationContext ctx;
    private final InjectionManager injectionManager;
    private final String beanName;

    private SpringManagedBeanFactory (ApplicationContext ctx, InjectionManager injectionManager, String beanName) {

      this.ctx = ctx;
      this.injectionManager = injectionManager;
      this.beanName = beanName;
    }

    @Override
    public Object get () {

      Object bean = ctx.getBean(beanName);
      if (bean instanceof Advised) {
        try {
          // Unwrap the bean and inject the values inside of it
          Object localBean = ((Advised)bean).getTargetSource().getTarget();
          injectionManager.inject(localBean);
        } catch (Exception e) {
          // Ignore and let the injection happen as it normally would.
          injectionManager.inject(bean);
        }
      } else {
        injectionManager.inject(bean);
      }
      return bean;
    }
  }
}
