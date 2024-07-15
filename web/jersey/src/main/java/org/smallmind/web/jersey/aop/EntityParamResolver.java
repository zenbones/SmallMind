/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

public class EntityParamResolver {

  private static final class EntityParamValueParamProvider implements ValueParamProvider {

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
  }

  public static final class EntityParamFeature implements Feature {

    @Override
    public boolean configure (FeatureContext context) {

      context.register(new AbstractBinder() {

        @Override
        protected void configure () {

          bind(EntityParamValueParamProvider.class).to(ValueParamProvider.class).in(Singleton.class);
        }
      });

      return true;
    }
  }
}
