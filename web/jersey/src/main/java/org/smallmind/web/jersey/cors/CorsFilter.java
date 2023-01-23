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
package org.smallmind.web.jersey.cors;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final String allowedHeaders;
  private final String exposedHeaders;

  public CorsFilter (String allowedHeaders, String exposedHeaders) {

    this.allowedHeaders = allowedHeaders;
    this.exposedHeaders = exposedHeaders;
  }

  @Override
  public void filter (ContainerRequestContext requestContext)
    throws IOException {

    if ("OPTIONS".equals(requestContext.getMethod())) {

      Response.ResponseBuilder responseBuilder = Response.ok();

      String originHeader;

      if (((originHeader = requestContext.getHeaderString("Origin")) != null) && (!originHeader.isEmpty())) {
        responseBuilder.header("Access-Control-Allow-Origin", originHeader);
      } else {
        responseBuilder.header("Access-Control-Allow-Origin", "*");
      }

      if (allowedHeaders != null) {
        responseBuilder.header("Access-Control-Allow-Headers", allowedHeaders);
      }
      if (exposedHeaders != null) {
        responseBuilder.header("Access-Control-Expose-Headers", exposedHeaders);
      }

      responseBuilder.header("Access-Control-Allow-Methods", "CONNECT,DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT");
      responseBuilder.header("Access-Control-Allow-Credentials", "true");

      requestContext.abortWith(responseBuilder.build());
    }
  }

  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

    String originHeader;

    if (((originHeader = requestContext.getHeaderString("Origin")) != null) && (!originHeader.isEmpty())) {
      responseContext.getHeaders().add("Access-Control-Allow-Origin", originHeader);
    } else {
      responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
    }

    if (allowedHeaders != null) {
      responseContext.getHeaders().add("Access-Control-Allow-Headers", allowedHeaders);
    }
    if (exposedHeaders != null) {
      responseContext.getHeaders().add("Access-Control-Expose-Headers", exposedHeaders);
    }

    responseContext.getHeaders().add("Access-Control-Allow-Methods", "CONNECT,DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT");
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
  }
}
