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
package org.smallmind.web.grizzly;

import org.glassfish.grizzly.http.server.Request;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit coverage for {@link DocumentRootHttpHandler#getRelativeURI(Request)}, exercising each prefix-stripping branch by
 * mocking the Grizzly {@link Request} request URI. The handler resolves a request path against its configured prefix,
 * returning {@code null} for traversal attempts, non-matching prefixes, and partial-segment false matches, {@code /}
 * for an exact prefix match, and the trailing remainder for a deeper path.
 */
@Test(groups = "unit")
public class DocumentRootHttpHandlerTest {

  private static final String PREFIX = "/app/document/files";

  private static Request requestFor (String requestURI) {

    Request request = Mockito.mock(Request.class);

    Mockito.when(request.getRequestURI()).thenReturn(requestURI);

    return request;
  }

  public void testTraversalAttemptResolvesToNull () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler(PREFIX, ".");

    Assert.assertNull(handler.getRelativeURI(requestFor(PREFIX + "/../secret.txt")));
  }

  public void testRequestNotUnderPrefixResolvesToNull () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler(PREFIX, ".");

    Assert.assertNull(handler.getRelativeURI(requestFor("/other/path/hello.txt")));
  }

  public void testExactPrefixResolvesToRoot () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler(PREFIX, ".");

    Assert.assertEquals(handler.getRelativeURI(requestFor(PREFIX)), "/");
  }

  public void testDeeperPathResolvesToRemainder () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler(PREFIX, ".");

    Assert.assertEquals(handler.getRelativeURI(requestFor(PREFIX + "/sub/hello.txt")), "/sub/hello.txt");
  }

  public void testPartialSegmentFalseMatchResolvesToNull () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler(PREFIX, ".");

    // The URI begins with the prefix string but the next character is not a separator, so the prefix does not match a
    // whole path segment and the remainder does not lead with a slash.
    Assert.assertNull(handler.getRelativeURI(requestFor(PREFIX + "X/y")));
  }

  public void testRootPrefixServesFullRequestURI () {

    // A "/" prefix is normalized to an empty prefix, so the full request URI is served from the document root.
    DocumentRootHttpHandler handler = new DocumentRootHttpHandler("/", ".");

    Assert.assertEquals(handler.getRelativeURI(requestFor("/hello.txt")), "/hello.txt");
  }

  public void testRootPrefixWithBareRootRequestResolvesToRoot () {

    DocumentRootHttpHandler handler = new DocumentRootHttpHandler("/", ".");

    Assert.assertEquals(handler.getRelativeURI(requestFor("")), "/");
  }
}
