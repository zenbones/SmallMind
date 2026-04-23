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
package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Embedded annotation on {@link CachedWith} that declares a fine-grained cache vector mutation
 * to perform when the managed durable is persisted.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Update {

  /**
   * Vector descriptor identifying the cache entry to insert into or remove from.
   */
  Vector value ();

  /**
   * Optional DAO method name returning {@code boolean} that gates whether this update is executed.
   */
  String filter () default "";

  /**
   * Optional DAO method name returning {@link OnPersist} that determines the insert/remove action;
   * defaults to {@link OnPersist#INSERT} when empty.
   */
  String onPersist () default "";

  /**
   * Finder that produces the durable instances to add to or remove from the vector.
   */
  Finder finder () default @Finder();

  /**
   * Optional proxy that transforms each found durable before the cache key is computed.
   */
  Proxy proxy () default @Proxy();
}
