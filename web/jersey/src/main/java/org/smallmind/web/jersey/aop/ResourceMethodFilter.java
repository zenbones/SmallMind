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
package org.smallmind.web.jersey.aop;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

/**
 * Jersey container filter, registered per {@link ResourceMethod}-annotated resource method by
 * {@link ResourceMethodDynamicFeature}, that registers the method's {@link JsonEntity} type before request processing
 * and clears all thread-local entity state on completion.
 */
public class ResourceMethodFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final Class<? extends JsonEntity> entityClass;

  /**
   * @param entityClass the {@link JsonEntity} class declared by the {@link ResourceMethod} annotation
   */
  public ResourceMethodFilter (Class<? extends JsonEntity> entityClass) {

    this.entityClass = entityClass;
  }

  /**
   * Stores the {@link JsonEntity} class for the matched resource method so it is available for parameter resolution
   * during the request.
   *
   * @param requestContext the incoming container request context
   */
  @Override
  public void filter (ContainerRequestContext requestContext) {

    EntityTranslator.storeEntityType(entityClass);
  }

  /**
   * Clears all thread-local entity state.
   *
   * @param requestContext  the container request context
   * @param responseContext the container response context
   */
  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

    EntityTranslator.clearEntity();
  }
}
