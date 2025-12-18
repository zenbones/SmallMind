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
package org.smallmind.web.json.query;

import java.util.Objects;

/**
 * Represents a permission rule (allowed, required, excluded, dependent) for a specific field, optionally scoped to an entity.
 */
public abstract class WherePermit {

  private final String name;
  private String entity;

  /**
   * Creates a permit scoped to a specific entity.
   *
   * @param entity entity alias
   * @param name   field name
   */
  public WherePermit (String entity, String name) {

    this(name);

    this.entity = entity;
  }

  /**
   * Creates a permit for the default entity context.
   *
   * @param name field name
   */
  public WherePermit (String name) {

    this.name = name;
  }

  /**
   * Factory for an allowed permit.
   *
   * @param entity entity alias
   * @param name   field name
   * @return permit instance
   */
  public static AllowedWherePermit allowed (String entity, String name) {

    return new AllowedWherePermit(entity, name);
  }

  /**
   * Factory for a required permit.
   *
   * @param entity entity alias
   * @param name   field name
   * @return permit instance
   */
  public static RequiredWherePermit required (String entity, String name) {

    return new RequiredWherePermit(entity, name);
  }

  /**
   * Factory for an excluded permit.
   *
   * @param entity entity alias
   * @param name   field name
   * @return permit instance
   */
  public static ExcludedWherePermit excluded (String entity, String name) {

    return new ExcludedWherePermit(entity, name);
  }

  /**
   * Factory for a dependent permit requiring another field.
   *
   * @param entity      entity alias
   * @param name        field name
   * @param requirement the required target field
   * @return permit instance
   */
  public static DependentWherePermit dependent (String entity, String name, TargetWherePermit requirement) {

    return new DependentWherePermit(entity, name, requirement);
  }

  /**
   * @return the permit type discriminator
   */
  public abstract PermitType getType ();

  /**
   * @return entity alias or {@code null} for default entity
   */
  public String getEntity () {

    return entity;
  }

  /**
   * @return field name governed by this permit
   */
  public String getName () {

    return name;
  }

  @Override
  public String toString () {

    return (entity == null) ? name : entity + '.' + name;
  }

  @Override
  public int hashCode () {

    return (entity == null) ? name.hashCode() : (31 * entity.hashCode()) + name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof WherePermit) && name.equals(((WherePermit)obj).getName()) && Objects.equals(entity, ((WherePermit)obj).getEntity());
  }
}
