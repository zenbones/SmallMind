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
package org.smallmind.web.jersey.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Jersey {@link DynamicFeature} that registers an {@link XmlJavaTypeAdapterFilter} against each resource method
 * annotated with {@link XmlJavaTypeAdapter}, leaving all other resource methods untouched. The declared adapter is
 * instantiated here, once, and handed to the filter.
 */
public class XmlJavaTypeAdapterDynamicFeature implements DynamicFeature {

  private final int priority;

  /**
   * @param priority the priority with which the filter is registered for each annotated resource method
   */
  public XmlJavaTypeAdapterDynamicFeature (int priority) {

    this.priority = priority;
  }

  /**
   * Registers {@link XmlJavaTypeAdapterFilter} for the resource method when it carries {@link XmlJavaTypeAdapter}.
   *
   * @param resourceInfo   the resource class and method being configured
   * @param featureContext the context for registering per-method providers
   * @throws IllegalStateException if the declared {@link XmlAdapter} cannot be created through a public no-argument constructor
   */
  @Override
  public void configure (ResourceInfo resourceInfo, FeatureContext featureContext) {

    Method method = resourceInfo.getResourceMethod();
    XmlJavaTypeAdapter xmlJavaTypeAdapter;

    if ((xmlJavaTypeAdapter = method.getAnnotation(XmlJavaTypeAdapter.class)) != null) {
      try {
        featureContext.register(new XmlJavaTypeAdapterFilter(xmlJavaTypeAdapter.value().getConstructor().newInstance()), priority);
      } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException exception) {
        throw new IllegalStateException("Unable to instantiate the adapter(" + xmlJavaTypeAdapter.value().getName() + ") declared on method(" + method + ")", exception);
      }
    }
  }
}
