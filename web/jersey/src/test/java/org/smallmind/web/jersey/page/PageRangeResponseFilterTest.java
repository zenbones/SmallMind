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
package org.smallmind.web.jersey.page;

import java.lang.reflect.Method;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.mockito.Mockito;
import org.smallmind.web.json.scaffold.util.Page;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link PageRangeResponseFilter} with a mocked {@link ResourceInfo}/{@link ContainerResponseContext}
 * to verify the {@code Content-Range} header and 200/206/416 status selection for a {@link PageRange}-annotated
 * method returning a {@link Page}.
 */
@Test(groups = "unit")
public class PageRangeResponseFilterTest {

  @PageRange
  public Page<String> annotatedResourceMethod () {

    return null;
  }

  private MultivaluedMap<String, Object> runFilter (Page<String> page)
    throws Exception {

    Method method = PageRangeResponseFilterTest.class.getMethod("annotatedResourceMethod");

    ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
    Mockito.when(resourceInfo.getResourceMethod()).thenReturn(method);

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
    Mockito.when(responseContext.getEntityClass()).thenReturn((Class)Page.class);
    Mockito.when(responseContext.getEntity()).thenReturn(page);
    Mockito.when(responseContext.getHeaders()).thenReturn(headers);

    PageRangeResponseFilter filter = new PageRangeResponseFilter();
    filter.resourceInfo = resourceInfo;
    filter.filter(null, responseContext);

    return new VerificationHeaders(headers, responseContext);
  }

  public void testCompleteRangeIs200 ()
    throws Exception {

    VerificationHeaders headers = (VerificationHeaders)runFilter(new Page<>(new String[10], 0L, 10, 10L));

    Mockito.verify(headers.responseContext).setStatus(200);
    Assert.assertEquals(String.valueOf(headers.getFirst("Content-Range")), "records 0-9/10");
  }

  public void testPartialRangeIs206 ()
    throws Exception {

    VerificationHeaders headers = (VerificationHeaders)runFilter(new Page<>(new String[10], 0L, 10, 50L));

    Mockito.verify(headers.responseContext).setStatus(206);
    Assert.assertEquals(String.valueOf(headers.getFirst("Content-Range")), "records 0-9/50");
  }

  public void testEmptyFirstPageIs200 ()
    throws Exception {

    VerificationHeaders headers = (VerificationHeaders)runFilter(new Page<>(new String[0], 0L, 10, 0L));

    Mockito.verify(headers.responseContext).setStatus(200);
    Assert.assertEquals(String.valueOf(headers.getFirst("Content-Range")), "records -/0");
  }

  public void testOutOfRangeIs416 ()
    throws Exception {

    VerificationHeaders headers = (VerificationHeaders)runFilter(new Page<>(new String[0], 100L, 10, 50L));

    Mockito.verify(headers.responseContext).setStatus(416);
    Assert.assertEquals(String.valueOf(headers.getFirst("Content-Range")), "records */50");
  }

  private static final class VerificationHeaders extends MultivaluedHashMap<String, Object> {

    private final transient ContainerResponseContext responseContext;

    private VerificationHeaders (MultivaluedMap<String, Object> delegate, ContainerResponseContext responseContext) {

      this.responseContext = responseContext;
      putAll(delegate);
    }
  }
}
