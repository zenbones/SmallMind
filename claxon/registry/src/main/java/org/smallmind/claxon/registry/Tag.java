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
package org.smallmind.claxon.registry;

/**
 * Represents an immutable key/value pair that is attached to meter readings to provide
 * dimensional metadata. Tags are commonly used by monitoring backends to filter, group, and
 * aggregate metrics (e.g., {@code env=production}, {@code service=auth}). Both the key and
 * the value must be non-null and non-empty; the constructor enforces this invariant.
 */
public class Tag {

  /**
   * The key component of this tag; never {@code null} or empty.
   */
  private final String key;

  /**
   * The value component of this tag; never {@code null} or empty.
   */
  private final String value;

  /**
   * Constructs a tag with the specified key and value, verifying that neither is {@code null}
   * nor empty.
   *
   * @param key   the tag key; must be non-null and non-empty
   * @param value the tag value; must be non-null and non-empty
   * @throws IllegalArgumentException if either {@code key} or {@code value} is {@code null} or empty
   */
  public Tag (String key, String value) {

    if ((key == null) || key.isEmpty() || (value == null) || value.isEmpty()) {
      throw new IllegalArgumentException("Both key and value must be neither null nor empty");
    }

    this.key = key;
    this.value = value;
  }

  /**
   * Returns the key component of this tag.
   *
   * @return the tag key; never {@code null} or empty
   */
  public String getKey () {

    return key;
  }

  /**
   * Returns the value component of this tag.
   *
   * @return the tag value; never {@code null} or empty
   */
  public String getValue () {

    return value;
  }

  /**
   * Returns a string representation of this tag in {@code key=value} format.
   *
   * @return the tag formatted as {@code key=value}
   */
  @Override
  public String toString () {

    return key + "=" + value;
  }

  /**
   * Returns a hash code derived from both the key and the value.
   *
   * @return hash code based on {@code key} and {@code value}
   */
  @Override
  public int hashCode () {

    return (key.hashCode() * 31) + value.hashCode();
  }

  /**
   * Compares this tag to another object for equality. Two tags are equal if and only if they
   * are both {@link Tag} instances with identical keys and values.
   *
   * @param obj the object to compare with this tag
   * @return {@code true} if {@code obj} is a {@link Tag} with the same key and value;
   * {@code false} otherwise
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Tag) && ((Tag)obj).getKey().equals(key) && ((Tag)obj).getValue().equals(value);
  }
}
