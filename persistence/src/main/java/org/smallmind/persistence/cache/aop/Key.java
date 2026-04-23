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
 * Embedded annotation that describes one segment of a cache vector key, referencing either
 * a method parameter, a durable field, or a literal constant.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Key {

  /**
   * Parameter or bean-property name that supplies the key value, or the literal constant itself when {@link #constant()} is {@code true}.
   * Defaults to {@code "id"}.
   */
  String value () default "id";

  /**
   * Optional label embedded in the cache key segment in place of the raw value name.
   */
  String alias () default "";

  /**
   * When {@code true}, {@link #value()} is used as a literal string constant rather than a parameter or field name.
   */
  boolean constant () default false;

  /**
   * When {@code true}, a {@code null} value for this key component is permitted; otherwise a null causes an error.
   */
  boolean nullable () default false;
}
