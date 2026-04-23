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

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;

/**
 * Programmatic {@link Key} implementation that can be constructed in code rather than as an annotation literal.
 */
public class KeyLiteral extends AnnotationLiteral<Key> implements Key {

  private final String value;
  private final String alias;
  private final boolean constant;
  private final boolean nullable;

  /**
   * Creates a key with the given field/parameter name and all other attributes at their defaults.
   *
   * @param value parameter or bean-property name for the key component
   */
  public KeyLiteral (String value) {

    this(value, "", false, false);
  }

  /**
   * Creates a key with a field/parameter name and an explicit alias.
   *
   * @param value parameter or bean-property name for the key component
   * @param alias label embedded in the cache key segment
   */
  public KeyLiteral (String value, String alias) {

    this(value, alias, false, false);
  }

  /**
   * Creates a key with a name, alias, and constant flag.
   *
   * @param value    parameter or bean-property name, or a literal string when {@code constant} is {@code true}
   * @param alias    label embedded in the cache key segment
   * @param constant {@code true} to treat {@code value} as a literal constant
   */
  public KeyLiteral (String value, String alias, boolean constant) {

    this(value, alias, constant, false);
  }

  /**
   * Creates a key with full control over all attributes.
   *
   * @param value    parameter or bean-property name, or a literal string when {@code constant} is {@code true}
   * @param alias    label embedded in the cache key segment
   * @param constant {@code true} to treat {@code value} as a literal constant
   * @param nullable {@code true} to allow a {@code null} value for this key component
   */
  public KeyLiteral (String value, String alias, boolean constant, boolean nullable) {

    this.value = value;
    this.alias = alias;
    this.constant = constant;
    this.nullable = nullable;
  }

  /**
   * Returns the parameter or field name, or the literal constant, depending on {@link #constant()}.
   *
   * @return key component source
   */
  @Override
  public String value () {

    return value;
  }

  /**
   * Returns the optional label embedded in the cache key segment.
   *
   * @return alias string, or empty when none was specified
   */
  @Override
  public String alias () {

    return alias;
  }

  /**
   * Returns whether {@link #value()} is a literal constant rather than a parameter or field name.
   *
   * @return {@code true} when the component is a constant
   */
  @Override
  public boolean constant () {

    return constant;
  }

  /**
   * Returns whether a {@code null} value is permitted for this key component.
   *
   * @return {@code true} if null is allowed
   */
  @Override
  public boolean nullable () {

    return nullable;
  }
}
