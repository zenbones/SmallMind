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
package org.smallmind.web.jersey.aop;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Provider
@ResourceMethod
public class ResourceMethodContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final ConcurrentHashMap<Class<? extends XmlAdapter>, XmlAdapter> ADAPTER_MAP = new ConcurrentHashMap<>();

  @Context
  ResourceInfo resourceInfo;

  @Override
  public void filter (ContainerRequestContext requestContext) {

    ResourceMethod resourceMethod;

    if ((resourceMethod = resourceInfo.getResourceMethod().getAnnotation(ResourceMethod.class)) != null) {
      EntityTranslator.storeEntityType(resourceMethod.value());
    }
  }

  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    throws IOException {

    XmlJavaTypeAdapter xmlJavaTypeAdapter;

    if ((xmlJavaTypeAdapter = resourceInfo.getResourceMethod().getAnnotation(XmlJavaTypeAdapter.class)) != null) {

      Class<? extends XmlAdapter> xmlAdapterClass;

      if ((xmlAdapterClass = ((XmlJavaTypeAdapter)xmlJavaTypeAdapter).value()) != null) {

        XmlAdapter xmlAdapter;

        if ((xmlAdapter = ADAPTER_MAP.get(xmlAdapterClass)) == null) {
          synchronized (ADAPTER_MAP) {
            if ((xmlAdapter = ADAPTER_MAP.get(xmlAdapterClass)) == null) {
              try {
                ADAPTER_MAP.put(xmlAdapterClass, xmlAdapter = xmlAdapterClass.newInstance());
              } catch (InstantiationException | IllegalAccessException exception) {
                throw new IOException(exception);
              }
            }
          }
        }

        try {
          responseContext.setEntity(xmlAdapter.marshal(responseContext.getEntity()));
        } catch (Exception exception) {
          throw new IOException(exception);
        }
      }
    }
  }
}
