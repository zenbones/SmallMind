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
package org.smallmind.web.jersey.page;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.smallmind.web.json.scaffold.util.Page;

@Provider
@PageRange
public class PageRangeContainerResponseFilter implements ContainerResponseFilter {

  public static int HTTP_DATA_COMPLETE = 200;
  public static int HTTP_DATA_INCOMPLETE = 206;
  public static int HTTP_DATA_OUT_OF_RANGE = 416;

  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

    Class<?> entityClass;

    if (((entityClass = responseContext.getEntityClass()) != null) && Page.class.isAssignableFrom(entityClass)) {

      Page<?> page = (Page<?>)responseContext.getEntity();

      if (page.getResultSize() == 0) {
        if (page.getFirstResult() == 0) {
          responseContext.getHeaders().add("Content-Range", "records -/0");
          responseContext.setStatus(HTTP_DATA_COMPLETE);
        } else {
          responseContext.getHeaders().add("Content-Range", "records */" + ((page.getTotalResults() < 0) ? '?' : page.getTotalResults()));
          responseContext.setStatus(HTTP_DATA_OUT_OF_RANGE);
        }
      } else {
        responseContext.getHeaders().add("Content-Range", new StringBuilder("records ").append(page.getFirstResult()).append('-').append(page.getFirstResult() + page.getResultSize() - 1).append('/').append((page.getTotalResults() < 0) ? '?' : page.getTotalResults()));
        responseContext.setStatus((page.getTotalResults() < 0) ? HTTP_DATA_INCOMPLETE : ((page.getFirstResult() + page.getResultSize()) == page.getTotalResults()) ? HTTP_DATA_COMPLETE : HTTP_DATA_INCOMPLETE);
      }
    }
  }
}
