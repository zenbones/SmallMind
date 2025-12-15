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
package org.smallmind.nutsnbolts.spring;

import java.util.Set;
import org.smallmind.nutsnbolts.util.Option;

/**
 * Provides typed access to resolved Spring property placeholders.
 */
public class SpringPropertyAccessor {

  private final Set<String> keySet;
  private final PropertyPlaceholderStringValueResolver stringValueResolver;

  /**
   * @param stringValueResolver resolver used to expand property placeholders
   */
  public SpringPropertyAccessor (PropertyPlaceholderStringValueResolver stringValueResolver) {

    this.stringValueResolver = stringValueResolver;

    keySet = stringValueResolver.getKeySet();
  }

  /**
   * @return the set of available property keys
   */
  public Set<String> getKeySet () {

    return keySet;
  }

  /**
   * Resolves the property as a string.
   *
   * @param key the property key
   * @return the resolved value or {@code null} if absent
   */
  public String asString (String key) {

    if (!keySet.contains(key)) {

      return null;
    }

    return stringValueResolver.resolveStringValue("${" + key + "}");
  }

  /**
   * Resolves the property as a boolean.
   *
   * @param key the property key
   * @return an {@link Option} containing the value when present
   */
  public Option<Boolean> asBoolean (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return Option.none();
    }

    return Option.of(Boolean.parseBoolean(stringValue));
  }

  /**
   * Resolves the property as a {@code long}.
   *
   * @param key the property key
   * @return an {@link Option} containing the parsed value
   * @throws RuntimeBeansException if the value cannot be parsed as a long
   */
  public Option<Long> asLong (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return Option.none();
    }

    try {
      return Option.of(Long.parseLong(stringValue));
    } catch (NumberFormatException numberFormatException) {
      throw new RuntimeBeansException("The value of key(%s) must interpolate as a long(%s)", key, stringValue);
    }
  }

  /**
   * Resolves the property as an {@code int}.
   *
   * @param key the property key
   * @return an {@link Option} containing the parsed value
   * @throws RuntimeBeansException if the value cannot be parsed as an int
   */
  public Option<Integer> asInt (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return Option.none();
    }

    try {
      return Option.of(Integer.parseInt(stringValue));
    } catch (NumberFormatException numberFormatException) {
      throw new RuntimeBeansException("The value of key(%s) must interpolate as an int(%s)", key, stringValue);
    }
  }
}
