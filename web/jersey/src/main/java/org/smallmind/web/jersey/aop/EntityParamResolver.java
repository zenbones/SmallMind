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
package org.smallmind.web.jersey.aop;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

@Singleton
public class EntityParamResolver {

  @Singleton
  public static final class EntityParamInjectionResolver extends ParamInjectionResolver<EntityParam> {

    public EntityParamInjectionResolver () {

      super(EntityParamValueFactoryProvider.class);
    }
  }

  @Singleton
  public static class EntityParamValueFactoryProvider extends AbstractValueFactoryProvider {

    @Inject
    public EntityParamValueFactoryProvider (final MultivaluedParameterExtractorProvider extractorProvider, final ServiceLocator injector) {

      super(extractorProvider, injector, Parameter.Source.UNKNOWN);
    }

    @Override
    protected Factory<?> createValueFactory (final Parameter parameter) {

      Class<?> paramClass;
      EntityParam entityParam;

      if (((paramClass = parameter.getRawType()) == null) || ((entityParam = parameter.getAnnotation(EntityParam.class)) == null)) {

        return null;
      }

      return new EntityParamRequestValueFactory(entityParam.value(), paramClass, new ParameterAnnotations(parameter.getAnnotations()));
    }
  }

  private static class EntityParamRequestValueFactory extends AbstractContainerRequestValueFactory<Object> {

    private ParameterAnnotations parameterAnnotations;
    private Class<?> paramClass;
    private String paramKey;

    public EntityParamRequestValueFactory (String paramKey, Class<?> paramClass, ParameterAnnotations parameterAnnotations) {

      this.paramKey = paramKey;
      this.paramClass = paramClass;
      this.parameterAnnotations = parameterAnnotations;
    }

    @Override
    public Object provide () {

      Object obj = EntityTranslator.getParameter(getContainerRequest(), paramKey, paramClass, parameterAnnotations);

      if ((obj == null) && paramClass.isPrimitive()) {
        throw new ParameterProcessingException("Attempt to assign a 'null' value to primitive argument(%s) of type(%s)", paramKey, paramClass.getSimpleName());
      }

      return obj;
    }
  }

  public static class Binder extends AbstractBinder {

    @Override
    protected void configure () {

      bind(EntityParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
      bind(EntityParamInjectionResolver.class).to(new TypeLiteral<InjectionResolver<EntityParam>>() {

      }).in(Singleton.class);
    }
  }
}
