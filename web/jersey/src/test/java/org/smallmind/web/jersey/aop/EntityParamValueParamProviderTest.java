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
package org.smallmind.web.jersey.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers the private {@code EntityParamValueParamProvider} nested inside {@link EntityParamResolver}, reached through
 * reflection because the production type is package-private to its enclosing container. Exercises the constant
 * {@code NORMAL} priority and all branches of {@code getValueProvider}: a {@code null} raw type, a parameter missing
 * the {@link EntityParam} annotation, and an entity-backed parameter whose returned function resolves a value from
 * the thread-local {@link EntityTranslator} entity. The thread-local state is reset around each method.
 */
@Test(groups = "unit")
public class EntityParamValueParamProviderTest {

  private ValueParamProvider valueParamProvider;

  @BeforeMethod
  public void setUp ()
    throws Exception {

    EntityTranslator.clearEntity();

    Class<?> providerClass = Class.forName("org.smallmind.web.jersey.aop.EntityParamResolver$EntityParamValueParamProvider");
    Constructor<?> constructor = providerClass.getDeclaredConstructor();

    constructor.setAccessible(true);
    valueParamProvider = (ValueParamProvider)constructor.newInstance();
  }

  @AfterMethod
  public void tearDown () {

    EntityTranslator.clearEntity();
  }

  private EntityParam entityParam (String key) {

    return new EntityParam() {

      @Override
      public Class<? extends Annotation> annotationType () {

        return EntityParam.class;
      }

      @Override
      public String value () {

        return key;
      }

      @Override
      public String comment () {

        return "";
      }
    };
  }

  public void testPriorityIsNormal () {

    Assert.assertEquals(valueParamProvider.getPriority(), ValueParamProvider.Priority.NORMAL);
  }

  public void testNullRawTypeReturnsNull () {

    Parameter parameter = Mockito.mock(Parameter.class);

    Mockito.when(parameter.getRawType()).thenReturn(null);

    Assert.assertNull(valueParamProvider.getValueProvider(parameter));
  }

  public void testMissingEntityParamReturnsNull () {

    Parameter parameter = Mockito.mock(Parameter.class);

    Mockito.when(parameter.getRawType()).thenReturn((Class)String.class);
    Mockito.when(parameter.getAnnotation(EntityParam.class)).thenReturn(null);

    Assert.assertNull(valueParamProvider.getValueProvider(parameter));
  }

  public void testEntityBackedParameterResolvesValue () {

    EntityParam annotation = entityParam("count");
    Parameter parameter = Mockito.mock(Parameter.class);

    Mockito.when(parameter.getRawType()).thenReturn((Class)Integer.class);
    Mockito.when(parameter.getAnnotation(EntityParam.class)).thenReturn(annotation);
    Mockito.when(parameter.getAnnotations()).thenReturn(new Annotation[] {annotation});

    Function<ContainerRequest, ?> valueProvider = valueParamProvider.getValueProvider(parameter);

    Assert.assertNotNull(valueProvider);

    Envelope envelope = new Envelope(new Argument("count", 42));
    ContainerRequest containerRequest = Mockito.mock(ContainerRequest.class);

    Mockito.when(containerRequest.readEntity(Envelope.class)).thenReturn(envelope);

    EntityTranslator.storeEntityType(Envelope.class);

    Assert.assertEquals(valueProvider.apply(containerRequest), Integer.valueOf(42));
  }
}
