/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Singleton;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;

@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {

  private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());

  private volatile ApplicationContext ctx;

  /**
   * Create a new instance.
   *
   * @param ctx Spring application context.
   */
  public AutowiredInjectResolver (ApplicationContext ctx) {

    this.ctx = ctx;
  }

  @Override
  public Object resolve (Injectee injectee) {

    AnnotatedElement parent = injectee.getParent();
    String beanName = null;
    if (parent != null) {
      Qualifier an = parent.getAnnotation(Qualifier.class);
      if (an != null) {
        beanName = an.value();
      }
    }
    boolean required = parent != null ? parent.getAnnotation(Autowired.class).required() : false;
    return getBeanFromSpringContext(beanName, injectee, required);
  }

  private Object getBeanFromSpringContext (String beanName, Injectee injectee, final boolean required) {

    try {
      DependencyDescriptor dependencyDescriptor = createSpringDependencyDescriptor(injectee);
      Set<String> autowiredBeanNames = new HashSet<>(1);
      autowiredBeanNames.add(beanName);
      return ctx.getAutowireCapableBeanFactory().resolveDependency(dependencyDescriptor, null,
        autowiredBeanNames, null);
    } catch (NoSuchBeanDefinitionException e) {
      if (required) {
        LOGGER.warning(e.getMessage());
        throw e;
      }
      return null;
    }
  }

  private DependencyDescriptor createSpringDependencyDescriptor (final Injectee injectee) {

    AnnotatedElement annotatedElement = injectee.getParent();

    if (annotatedElement.getClass().isAssignableFrom(Field.class)) {
      return new DependencyDescriptor((Field)annotatedElement, !injectee.isOptional());
    } else if (annotatedElement.getClass().isAssignableFrom(Method.class)) {
      return new DependencyDescriptor(
        new MethodParameter((Method)annotatedElement, injectee.getPosition()), !injectee.isOptional());
    } else {
      return new DependencyDescriptor(
        new MethodParameter((Constructor)annotatedElement, injectee.getPosition()), !injectee.isOptional());
    }
  }

  @Override
  public boolean isConstructorParameterIndicator () {

    return false;
  }

  @Override
  public boolean isMethodParameterIndicator () {

    return false;
  }

  @Override
  public Class<Autowired> getAnnotation () {

    return Autowired.class;
  }
}

