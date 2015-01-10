/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.jersey.spring;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@Singleton
public class AutowiredInjectionResolver implements InjectionResolver<Autowired> {

  private ApplicationContext applicationContext;

  public AutowiredInjectionResolver (ApplicationContext applicationContext) {

    this.applicationContext = applicationContext;
  }

  @Override
  public Object resolve (Injectee injectee, ServiceHandle<?> root) {

    return getBeanFromSpringContext(injectee.getRequiredType());
  }

  @Override
  public boolean isConstructorParameterIndicator () {

    return false;
  }

  @Override
  public boolean isMethodParameterIndicator () {

    return false;
  }

  private Object getBeanFromSpringContext (Type beanType) {

    Map<String, ?> beans = applicationContext.getBeansOfType(getClassFromType(beanType));

    if (!beans.values().isEmpty()) {
      return beans.values().iterator().next();
    }

    return null;
  }

  private Class<?> getClassFromType (Type type) {

    if (type instanceof Class) {

      return (Class<?>)type;
    }
    if (type instanceof ParameterizedType) {

      return (Class<?>)((ParameterizedType)type).getRawType();
    }

    return null;
  }

}