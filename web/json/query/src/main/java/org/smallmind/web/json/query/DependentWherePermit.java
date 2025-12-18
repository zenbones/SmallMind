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

public class DependentWherePermit extends WherePermit {

  private final TargetWherePermit requirement;

  /**
   * Creates a dependency permit scoped to an entity, indicating the required target field.
   *
   * @param entity      entity alias for the dependent field
   * @param name        dependent field name
   * @param requirement the required target field
   */
  public DependentWherePermit (String entity, String name, TargetWherePermit requirement) {

    super(entity, name);

    this.requirement = requirement;
  }

  /**
   * Creates a dependency permit in the default entity context.
   *
   * @param name        dependent field name
   * @param requirement the required target field
   */
  public DependentWherePermit (String name, TargetWherePermit requirement) {

    super(name);

    this.requirement = requirement;
  }

  /**
   * @return {@link PermitType#DEPENDENT}
   */
  @Override
  public PermitType getType () {

    return PermitType.DEPENDENT;
  }

  /**
   * @return the required target that must accompany this dependent field
   */
  public TargetWherePermit getRequirement () {

    return requirement;
  }

  /**
   * Presents a readable description combining the base permit with its required target.
   *
   * @return string in the form "{base} requires {requirement}"
   */
  @Override
  public String toString () {

    return super.toString() + " requires " + getRequirement().toString();
  }
}
