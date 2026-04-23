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
package org.smallmind.nutsnbolts.resource;

/**
 * Base {@link Resource} implementation that stores the path component and provides
 * identifier composition, string representation, and value-based equality using the
 * {@code scheme:path} convention.
 */
public abstract class AbstractResource implements Resource {

  private final String path;

  /**
   * Constructs an {@code AbstractResource} with the given path component.
   *
   * @param path raw path portion of the resource identifier
   */
  public AbstractResource (String path) {

    this.path = path;
  }

  /**
   * Composes a unique identifier by joining the scheme and path with a colon separator.
   *
   * @return identifier string of the form {@code scheme:path}
   */
  public String getIdentifier () {

    return getScheme() + ":" + getPath();
  }

  /**
   * Returns the raw path component supplied at construction time.
   *
   * @return path portion of the resource identifier
   */
  public String getPath () {

    return path;
  }

  /**
   * Returns the full resource identifier string, equivalent to {@link #getIdentifier()}.
   *
   * @return resource identifier
   */
  public String toString () {

    return getIdentifier();
  }

  /**
   * Computes a hash code derived from both the scheme and path to support use in maps and sets.
   *
   * @return hash code based on scheme and path
   */
  @Override
  public int hashCode () {

    return (getScheme().hashCode() * 31) + path.hashCode();
  }

  /**
   * Compares this resource to another for equality based on scheme and path.
   *
   * @param obj the object to compare against this resource
   * @return {@code true} if {@code obj} is an {@link AbstractResource} with the same scheme and path; {@code false} otherwise
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof AbstractResource) && ((AbstractResource)obj).getScheme().equals(getScheme()) && ((AbstractResource)obj).getPath().equals(path);
  }
}
