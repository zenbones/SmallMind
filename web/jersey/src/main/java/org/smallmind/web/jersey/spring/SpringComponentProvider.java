/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import javax.ws.rs.Path;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.context.ApplicationContext;

public class SpringComponentProvider implements ComponentProvider {

  private ServiceLocator serviceLocator;
  private ApplicationContext applicationContext;

  @Override
  public void initialize (ServiceLocator serviceLocator) {

    LoggerManager.getLogger(SpringComponentProvider.class).info("Searching for Spring application context on Thread(%s)", Thread.currentThread().getName());
    if ((applicationContext = ExposedApplicationContext.getApplicationContext()) == null) {
      throw new SpringHK2IntegrationException("Spring application context has not been created prior to HK2 application initialization");
    }

    this.serviceLocator = serviceLocator;

    // initialize HK2 spring-bridge
    SpringBridge.getSpringBridge().initializeSpringBridge(serviceLocator);
    SpringIntoHK2Bridge springBridge = serviceLocator.getService(SpringIntoHK2Bridge.class);
    springBridge.bridgeSpringBeanFactory(applicationContext);

    // register Spring @Autowired annotation handler with HK2 ServiceLocator
    ServiceLocatorUtilities.addOneConstant(serviceLocator, new AutowiredInjectionResolver(applicationContext));
    ServiceLocatorUtilities.addOneConstant(serviceLocator, applicationContext, "SpringContext", ApplicationContext.class);
  }

  @Override
  public boolean bind (Class<?> component, Set<Class<?>> providerContracts) {

    if (component.isAnnotationPresent(Path.class)) {

      String[] beanNames = applicationContext.getBeanNamesForType(component);

      if ((beanNames == null) || (beanNames.length == 0)) {
        LoggerManager.getLogger(SpringComponentProvider.class).warn("The Spring context failed to contain a bean of type(%s) - unable to bind into HK2 ", component.getName());
      } else if (beanNames.length > 1) {
        LoggerManager.getLogger(SpringComponentProvider.class).warn("The Spring context contained multiple beans of type(%s) - unable to bind into HK2", component.getName());
      } else {

        DynamicConfiguration dynamicConfiguration = Injections.getConfiguration(serviceLocator);
        ServiceBindingBuilder serviceBindingBuilder = Injections.newFactoryBinder(new SpringManagedBeanFactory(serviceLocator, applicationContext, beanNames[0]));

        serviceBindingBuilder.to(component);
        Injections.addBinding(serviceBindingBuilder, dynamicConfiguration);
        dynamicConfiguration.commit();

        LoggerManager.getLogger(SpringComponentProvider.class).info("Bound the Spring bean(%s) into the HK2 context", beanNames[0]);

        return true;
      }
    }

    return false;
  }

  @Override
  public void done () {

  }

  private static class SpringManagedBeanFactory implements Factory {

    private ApplicationContext applicationContext;
    private ServiceLocator serviceLocator;
    private String beanName;
    private boolean singleton;

    private SpringManagedBeanFactory (ServiceLocator serviceLocator, ApplicationContext applicationContext, String beanName) {

      this.serviceLocator = serviceLocator;
      this.applicationContext = applicationContext;
      this.beanName = beanName;

      if ((singleton = applicationContext.isSingleton(beanName))) {
        serviceLocator.inject(applicationContext.getBean(beanName));
      }
    }

    @Override
    public Object provide () {

      Object bean = applicationContext.getBean(beanName);

      if (!singleton) {
        serviceLocator.inject(bean);
      }

      return bean;
    }

    @Override
    public void dispose (Object instance) {

    }
  }
}
