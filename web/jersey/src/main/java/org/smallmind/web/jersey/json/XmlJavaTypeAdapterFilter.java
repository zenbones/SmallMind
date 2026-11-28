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

import java.io.IOException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Jersey response filter, registered per {@link XmlJavaTypeAdapter}-annotated resource method by
 * {@link XmlJavaTypeAdapterDynamicFeature}, that marshals the response entity through the method's adapter.
 */
public class XmlJavaTypeAdapterFilter implements ContainerResponseFilter {

  private final XmlAdapter xmlAdapter;

  /**
   * @param xmlAdapter the adapter used to marshal the response entity
   */
  public XmlJavaTypeAdapterFilter (XmlAdapter xmlAdapter) {

    this.xmlAdapter = xmlAdapter;
  }

  /**
   * Replaces the response entity with the result of marshalling it through the configured {@link XmlAdapter}.
   *
   * @param requestContext  the container request context
   * @param responseContext the container response context whose entity is replaced
   * @throws IOException if the marshal operation fails
   */
  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    throws IOException {

    try {
      responseContext.setEntity(xmlAdapter.marshal(responseContext.getEntity()));
    } catch (Exception exception) {
      throw new IOException(exception);
    }
  }
}
