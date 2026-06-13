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

import java.io.ByteArrayInputStream;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.mockito.Mockito;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;
import org.smallmind.web.json.scaffold.fault.ResourceInvocationException;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link FaultTranslatingClientResponseFilter} with a mocked {@link ClientResponseContext}, covering the
 * pass-through of out-of-range and non-JSON responses, the native-Java fault path that rethrows the original
 * throwable wrapped in a {@link ResourceInvocationException}, and the non-native fault path that yields a
 * {@link FaultWrappingException}.
 */
@Test(groups = "unit")
public class FaultTranslatingClientResponseFilterTest {

  private ClientResponseContext mockResponse (int status, MediaType mediaType, boolean hasEntity, byte[] body) {

    ClientResponseContext responseContext = Mockito.mock(ClientResponseContext.class);

    Mockito.when(responseContext.getStatus()).thenReturn(status);
    Mockito.when(responseContext.getMediaType()).thenReturn(mediaType);
    Mockito.when(responseContext.hasEntity()).thenReturn(hasEntity);
    Mockito.when(responseContext.getEntityStream()).thenReturn((body == null) ? null : new ByteArrayInputStream(body));

    return responseContext;
  }

  public void testSuccessStatusPassesThrough ()
    throws Exception {

    new FaultTranslatingClientResponseFilter().filter(null, mockResponse(200, MediaType.APPLICATION_JSON_TYPE, true, new byte[] {'{', '}'}));
  }

  public void testNonJsonErrorPassesThrough ()
    throws Exception {

    new FaultTranslatingClientResponseFilter().filter(null, mockResponse(500, MediaType.TEXT_PLAIN_TYPE, true, new byte[] {'x'}));
  }

  public void testNoEntityPassesThrough ()
    throws Exception {

    new FaultTranslatingClientResponseFilter().filter(null, mockResponse(500, MediaType.APPLICATION_JSON_TYPE, false, null));
  }

  public void testNativeJavaFaultRethrowsOriginal ()
    throws Exception {

    Fault fault = new Fault(new IllegalStateException("native boom"));
    byte[] body = JsonCodec.writeAsBytes(fault);

    try {
      new FaultTranslatingClientResponseFilter().filter(null, mockResponse(500, MediaType.APPLICATION_JSON_TYPE, true, body));
      Assert.fail("Expected a ResourceInvocationException");
    } catch (ResourceInvocationException resourceInvocationException) {
      Assert.assertTrue(resourceInvocationException.getCause() instanceof IllegalStateException);
      Assert.assertEquals(resourceInvocationException.getCause().getMessage(), "native boom");
    }
  }

  public void testNonNativeFaultBecomesFaultWrappingException ()
    throws Exception {

    Fault fault = new Fault(new RuntimeException("no native"), false);
    byte[] body = JsonCodec.writeAsBytes(fault);

    try {
      new FaultTranslatingClientResponseFilter().filter(null, mockResponse(404, MediaType.APPLICATION_JSON_TYPE, true, body));
      Assert.fail("Expected a FaultWrappingException");
    } catch (FaultWrappingException faultWrappingException) {
      Assert.assertNotNull(faultWrappingException.getFault());
      Assert.assertEquals(faultWrappingException.getFault().getThrowableType(), RuntimeException.class.getName());
    }
  }

  public void testCustomBoundsExcludeStatus ()
    throws Exception {

    new FaultTranslatingClientResponseFilter(500, 600).filter(null, mockResponse(404, MediaType.APPLICATION_JSON_TYPE, true, new byte[] {'{', '}'}));
  }
}
