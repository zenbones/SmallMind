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
package org.smallmind.phalanx.wire.transport;

/**
 * Describes a method argument defined in code rather than discovered through reflection annotations,
 * used when constructing {@link org.smallmind.phalanx.wire.Methodology} entries for synthetic methods
 * such as {@code equals}.
 */
public class SyntheticArgument {

  private final Class<?> parameterType;
  private final String name;

  /**
   * Constructs a synthetic argument with the given name and type.
   *
   * @param name          logical parameter name used in wire argument maps
   * @param parameterType declared type of the parameter
   */
  public SyntheticArgument (String name, Class<?> parameterType) {

    this.name = name;
    this.parameterType = parameterType;
  }

  /**
   * Returns the logical parameter name.
   *
   * @return parameter name
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the declared type of this parameter.
   *
   * @return parameter type
   */
  public Class<?> getParameterType () {

    return parameterType;
  }
}
