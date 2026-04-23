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

import org.glassfish.jersey.server.ContainerRequest;
import org.smallmind.nutsnbolts.reflection.MissingAnnotationException;

/**
 * Thread-local coordinator that deserializes the request body into a {@link JsonEntity} once per request and
 * provides typed parameter extraction for {@link EntityParam}-annotated resource method arguments.
 */
public class EntityTranslator {

  private static final ThreadLocal<JsonEntity> JSON_ENTITY_LOCAL = new ThreadLocal<>();
  private static final ThreadLocal<Class<? extends JsonEntity>> JSON_ENTITY_CLASS_LOCAL = new ThreadLocal<>();

  /**
   * Registers the concrete {@link JsonEntity} type to deserialize for the current request thread.
   *
   * @param clazz concrete {@link JsonEntity} implementation class
   * @param <E>   entity type
   */
  public static <E extends JsonEntity> void storeEntityType (Class<E> clazz) {

    JSON_ENTITY_CLASS_LOCAL.set(clazz);
  }

  /**
   * Lazily deserializes the request body into the registered {@link JsonEntity} type and delegates parameter lookup
   * to it, caching the entity instance for the duration of the request.
   *
   * @param containerRequest     the current Jersey container request containing the entity body
   * @param key                  parameter key to look up
   * @param clazz                expected parameter type
   * @param parameterAnnotations annotations on the target parameter
   * @param <T>                  desired return type
   * @return the typed parameter value extracted from the entity
   * @throws MissingAnnotationException   if no entity type was registered via {@link #storeEntityType}
   * @throws ParameterProcessingException if the registered type is the raw {@link JsonEntity} interface rather than
   *                                      a concrete implementation
   */
  public static <T> T getParameter (ContainerRequest containerRequest, String key, Class<T> clazz, ParameterAnnotations parameterAnnotations) {

    JsonEntity jsonEntity;

    if ((jsonEntity = JSON_ENTITY_LOCAL.get()) == null) {

      Class<? extends JsonEntity> entityClass;

      if ((entityClass = JSON_ENTITY_CLASS_LOCAL.get()) == null) {
        throw new MissingAnnotationException("Missing annotation(%s)", ResourceMethod.class.getName());
      } else if (JsonEntity.class.equals(entityClass)) {
        throw new ParameterProcessingException("The @%s annotation must define an implementation of %s", ResourceMethod.class.getSimpleName(), JsonEntity.class.getSimpleName());
      }

      JSON_ENTITY_LOCAL.set(jsonEntity = containerRequest.readEntity(entityClass));
    }

    try {
      return jsonEntity.getParameter(key, clazz, parameterAnnotations);
    } catch (Throwable throwable) {
      JSON_ENTITY_LOCAL.remove();

      throw throwable;
    }
  }

  /**
   * Removes the thread-local entity instance and entity class, cleaning up after the current request completes.
   */
  public static void clearEntity () {

    JSON_ENTITY_LOCAL.remove();
    JSON_ENTITY_CLASS_LOCAL.remove();
  }
}
