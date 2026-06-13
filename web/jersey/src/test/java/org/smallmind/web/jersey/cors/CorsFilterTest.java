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
package org.smallmind.web.jersey.cors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link CorsFilter}'s request and response methods with mocked JAX-RS contexts to verify OPTIONS
 * preflight short-circuiting and the CORS headers stamped onto regular responses, including the
 * configured-Origin echo versus wildcard fallback.
 */
@Test(groups = "unit")
public class CorsFilterTest {

  private final CorsFilter corsFilter = new CorsFilter("Content-Type, Authorization", "Location");

  public void testPreflightIsAbortedWithCorsResponse ()
    throws Exception {

    ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
    Mockito.when(requestContext.getMethod()).thenReturn("OPTIONS");
    Mockito.when(requestContext.getHeaderString("Origin")).thenReturn("https://app.example.com");

    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

    corsFilter.filter(requestContext);

    Mockito.verify(requestContext).abortWith(responseCaptor.capture());

    Response response = responseCaptor.getValue();

    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getHeaderString("Access-Control-Allow-Origin"), "https://app.example.com");
    Assert.assertEquals(response.getHeaderString("Access-Control-Allow-Headers"), "Content-Type, Authorization");
    Assert.assertEquals(response.getHeaderString("Access-Control-Expose-Headers"), "Location");
    Assert.assertTrue(response.getHeaderString("Access-Control-Allow-Methods").contains("POST"));
  }

  public void testNonOptionsRequestIsNotAborted ()
    throws Exception {

    ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
    Mockito.when(requestContext.getMethod()).thenReturn("GET");

    corsFilter.filter(requestContext);

    Mockito.verify(requestContext, Mockito.never()).abortWith(Mockito.any());
  }

  public void testResponseHeadersEchoOrigin () {

    ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
    Mockito.when(requestContext.getMethod()).thenReturn("GET");
    Mockito.when(requestContext.getHeaderString("Origin")).thenReturn("https://app.example.com");

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
    Mockito.when(responseContext.getHeaders()).thenReturn(headers);

    corsFilter.filter(requestContext, responseContext);

    Assert.assertEquals(headers.getFirst("Access-Control-Allow-Origin"), "https://app.example.com");
    Assert.assertEquals(headers.getFirst("Access-Control-Allow-Headers"), "Content-Type, Authorization");
    Assert.assertEquals(headers.getFirst("Access-Control-Allow-Credentials"), "true");
  }

  public void testResponseHeadersWildcardWhenNoOrigin () {

    ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
    Mockito.when(requestContext.getMethod()).thenReturn("GET");
    Mockito.when(requestContext.getHeaderString("Origin")).thenReturn(null);

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
    Mockito.when(responseContext.getHeaders()).thenReturn(headers);

    corsFilter.filter(requestContext, responseContext);

    Assert.assertEquals(headers.getFirst("Access-Control-Allow-Origin"), "*");
  }
}
