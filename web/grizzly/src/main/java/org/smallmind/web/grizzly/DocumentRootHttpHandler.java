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
import org.glassfish.grizzly.http.server.StaticHttpHandler;

/**
 * A {@link StaticHttpHandler} that serves its document root beneath the full multi-segment request prefix it is
 * registered under. Grizzly's handler registration treats only the first path segment of a mapping as the strippable
 * context, so a plain {@code StaticHttpHandler} registered at a path such as {@code /app/document/files} would resolve
 * requests against the document root using everything after the first segment. This handler instead removes the entire
 * configured prefix before resolving the request, so a document root mapped to {@code /app/document/files} serves its
 * contents at that full path.
 */
public class DocumentRootHttpHandler extends StaticHttpHandler {

  private final String prefix;

  /**
   * Constructs a handler that serves the given document roots beneath the supplied request prefix.
   *
   * @param prefix   the full request path prefix the handler is registered under; a value of {@code /} is treated as
   *                 no prefix, serving the document root at the server root
   * @param docRoots one or more document root locations passed through to {@link StaticHttpHandler}
   */
  public DocumentRootHttpHandler (String prefix, String... docRoots) {

    super(docRoots);

    this.prefix = "/".equals(prefix) ? "" : prefix;
  }

  /**
   * Resolves the request path relative to the configured prefix. Requests that escape the document root with
   * {@code ..} or that do not fall under the prefix yield {@code null}, signalling a missing resource; a request that
   * matches the prefix exactly resolves to {@code /}, and any deeper request resolves to the portion of the path that
   * follows the prefix.
   *
   * @param request the current request
   * @return the path relative to the document root, or {@code null} if the request does not address this handler's
   *         document root
   */
  @Override
  protected String getRelativeURI (Request request) {

    String requestURI = request.getRequestURI();

    if (requestURI.contains("..") || (!requestURI.startsWith(prefix))) {

      return null;
    } else {

      String relativeURI = requestURI.substring(prefix.length());

      if (relativeURI.isEmpty()) {

        return "/";
      }

      return (relativeURI.charAt(0) == '/') ? relativeURI : null;
    }
  }
}
