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
 * Defines a single component of a cache key used by {@link VectorCalculator}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Key {

  /**
   * Name of the method parameter or field contributing to the key.
   *
   * @return parameter or field identifier, or literal value when {@link #constant()} is true
   */
  String value () default "id";

  /**
   * Alternate label to embed within the generated key segment.
   *
   * @return alias applied to the key component
   */
  String alias () default "";

  /**
   * Indicates whether {@link #value()} represents a literal constant instead of a parameter or field name.
   *
   * @return {@code true} when the key component is a constant
   */
  boolean constant () default false;

  /**
   * Specifies whether a null value is allowed for this key component.
   *
   * @return {@code true} if the key may include a null value
   */
  boolean nullable () default false;
}
