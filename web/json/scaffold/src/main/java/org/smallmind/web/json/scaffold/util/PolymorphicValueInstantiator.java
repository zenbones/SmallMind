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
package org.smallmind.web.json.scaffold.util;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.reflection.AnnotationFilter;
import org.smallmind.nutsnbolts.reflection.OffloadingInvocationHandler;
import org.smallmind.nutsnbolts.reflection.PassType;
import org.smallmind.nutsnbolts.reflection.ProxyGenerator;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdValueInstantiator;

/**
 * Jackson value instantiator that returns a proxy wrapping the real polymorphic instance stored in
 * a thread-local by {@link AttributedPolymorphicXmlAdapter}, ensuring deserialized properties are
 * applied to the correct object.
 */
public class PolymorphicValueInstantiator extends StdValueInstantiator {

  private static final ThreadLocal<Object> polymorphicInstanceThreadLocal = new ThreadLocal<>();
  private final Class<?> polymorphicSubClass;

  /**
   * Creates the instantiator for the given deserialization configuration and target polymorphic subclass.
   *
   * @param deserializationConfig deserialization configuration from the Jackson module setup
   * @param javaType              Java type of the bean reference being bound
   * @param polymorphicSubClass   concrete subclass to instantiate
   */
  public PolymorphicValueInstantiator (DeserializationConfig deserializationConfig, JavaType javaType, Class<?> polymorphicSubClass) {

    super(deserializationConfig, javaType);

    this.polymorphicSubClass = polymorphicSubClass;
  }

  /**
   * Stores the target polymorphic instance in the thread-local so it can be wrapped during creation.
   *
   * @param obj polymorphic instance to use as the deserialization target
   */
  public static void setPolymorphicInstance (Object obj) {

    polymorphicInstanceThreadLocal.set(obj);
  }

  /**
   * @return {@code true} because this instantiator supports default (no-arg) creation via the thread-local
   */
  @Override
  public boolean canCreateUsingDefault () {

    return true;
  }

  /**
   * Returns a proxy that forwards to the polymorphic instance held in the thread-local, then clears it.
   *
   * @param ctxt deserialization context
   * @return proxy delegating to the stored polymorphic instance
   * @throws JAXBProcessingException if no polymorphic instance has been set in the thread-local
   */
  @Override
  public Object createUsingDefault (DeserializationContext ctxt) {

    Object polymorphicInstance;

    if ((polymorphicInstance = polymorphicInstanceThreadLocal.get()) == null) {
      throw new JAXBProcessingException("Can not update a 'null' instance of the polymorphic sub-class(%s)", polymorphicSubClass.getName());
    } else {
      try {
        return ProxyGenerator.createProxy(polymorphicSubClass, new OffloadingInvocationHandler(polymorphicInstance), new AnnotationFilter(PassType.EXCLUDE, XmlJavaTypeAdapter.class));
      } finally {
        polymorphicInstanceThreadLocal.remove();
      }
    }
  }
}
