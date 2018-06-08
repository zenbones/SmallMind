/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.util.function.Function;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

@Singleton
final class EntityParamValueParamProvider implements ValueParamProvider {

  @Override
  public PriorityType getPriority () {

    return Priority.NORMAL;
  }

  @Override
  public Function<ContainerRequest, ?> getValueProvider (Parameter parameter) {

    Class<?> paramClass;
    EntityParam entityParam;

    if (((paramClass = parameter.getRawType()) == null) || ((entityParam = parameter.getAnnotation(EntityParam.class)) == null)) {

      return null;
    }

    return (containerRequest) -> EntityTranslator.getParameter(containerRequest, entityParam.value(), paramClass, new ParameterAnnotations(parameter.getAnnotations()));
  }

  public static class Binder extends AbstractBinder {

    @Override
    protected void configure () {

      Provider<ContainerRequest> requestProvider = createManagedInstanceProvider(ContainerRequest.class);

      EntityParamValueParamProvider valueSupplier = new EntityParamValueParamProvider();
      bind(Bindings.service(valueSupplier).to(ValueParamProvider.class));
      bind(Bindings.injectionResolver(new ParamInjectionResolver<>(valueSupplier, EntityParam.class, requestProvider)));
    }
  }
}