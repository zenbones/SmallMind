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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link ThrowableExceptionMapper} dispatch order: a matching delegate
 * {@link ConcreteExceptionMapper} wins, a {@link jakarta.ws.rs.WebApplicationException} response passes
 * through unchanged, and everything else becomes a 500 JSON {@link Fault}.
 */
@Test(groups = "unit")
public class ThrowableExceptionMapperTest {

  public void testWebApplicationExceptionPassesThrough () {

    Response response = new ThrowableExceptionMapper().toResponse(new NotFoundException());

    Assert.assertEquals(response.getStatus(), 404);
  }

  public void testUnclassifiedBecomesJsonFault () {

    Response response = new ThrowableExceptionMapper().toResponse(new RuntimeException("boom"));

    Assert.assertEquals(response.getStatus(), 500);
    Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
    Assert.assertTrue(response.getEntity() instanceof Fault);
  }

  public void testMatchingDelegateMapperWins () {

    Response response = new ThrowableExceptionMapper(new TeapotMapper()).toResponse(new IllegalStateException("brewing"));

    Assert.assertEquals(response.getStatus(), 418);
  }

  public void testNonMatchingDelegateFallsThrough () {

    Response response = new ThrowableExceptionMapper(new TeapotMapper()).toResponse(new IllegalArgumentException("nope"));

    Assert.assertEquals(response.getStatus(), 500);
    Assert.assertTrue(response.getEntity() instanceof Fault);
  }

  public static class TeapotMapper extends ConcreteExceptionMapper<IllegalStateException> {

    @Override
    public Response toResponse (IllegalStateException exception) {

      return Response.status(418).build();
    }
  }
}
