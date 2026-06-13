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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the three {@link FormattedWebApplicationException} constructors: the {@code String.format} message path
 * (including the {@code null} message branch), the throwable-wrapping path, and the pre-built {@link Fault} path.
 * Each test asserts the resulting status code, the {@code application/json} content type, the {@link Fault} entity,
 * and the exception message derived from the fault.
 */
@Test(groups = "unit")
public class FormattedWebApplicationExceptionTest {

  public void testFormattedMessageConstructor () {

    FormattedWebApplicationException exception = new FormattedWebApplicationException(Response.Status.BAD_REQUEST, "value(%s) was rejected", "abc");
    Response response = exception.getResponse();

    Assert.assertEquals(response.getStatus(), 400);
    Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
    Assert.assertTrue(response.getEntity() instanceof Fault);
    Assert.assertEquals(((Fault)response.getEntity()).getMessage(), "value(abc) was rejected");
    Assert.assertEquals(exception.getMessage(), "value(abc) was rejected");
  }

  public void testNullMessageConstructor () {

    FormattedWebApplicationException exception = new FormattedWebApplicationException(Response.Status.NOT_FOUND, (String)null);
    Response response = exception.getResponse();

    Assert.assertEquals(response.getStatus(), 404);
    Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
    Assert.assertTrue(response.getEntity() instanceof Fault);
    Assert.assertNull(((Fault)response.getEntity()).getMessage());
    Assert.assertNull(exception.getMessage());
  }

  public void testThrowableConstructor () {

    IllegalStateException cause = new IllegalStateException("boom");
    FormattedWebApplicationException exception = new FormattedWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, cause);
    Response response = exception.getResponse();
    Fault fault = (Fault)response.getEntity();

    Assert.assertEquals(response.getStatus(), 500);
    Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
    Assert.assertEquals(fault.getThrowableType(), IllegalStateException.class.getName());
    Assert.assertEquals(fault.getMessage(), "boom");
    // Native encoding is suppressed by the (throwable, false) fault construction.
    Assert.assertNull(fault.getNativeObject());
    Assert.assertEquals(exception.getMessage(), "boom");
  }

  public void testFaultConstructor () {

    Fault fault = new Fault("prepared message");
    FormattedWebApplicationException exception = new FormattedWebApplicationException(Response.Status.CONFLICT, fault);
    Response response = exception.getResponse();

    Assert.assertEquals(response.getStatus(), 409);
    Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
    Assert.assertSame(response.getEntity(), fault);
    Assert.assertEquals(exception.getMessage(), "prepared message");
  }
}
