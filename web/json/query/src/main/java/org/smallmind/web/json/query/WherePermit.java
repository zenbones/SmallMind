/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public abstract class WherePermit {

  private final String name;
  private String entity;

  public WherePermit (String entity, String name) {

    this(name);

    this.entity = entity;
  }

  public WherePermit (String name) {

    this.name = name;
  }

  public static AllowedWherePermit allowed (String entity, String name) {

    return new AllowedWherePermit(entity, name);
  }

  public static RequiredWherePermit required (String entity, String name) {

    return new RequiredWherePermit(entity, name);
  }

  public static ExcludedWherePermit excluded (String entity, String name) {

    return new ExcludedWherePermit(entity, name);
  }

  public static DependentWherePermit dependent (String entity, String name, TargetWherePermit requirement) {

    return new DependentWherePermit(entity, name, requirement);
  }

  public abstract PermitType getType ();

  public String getEntity () {

    return entity;
  }

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
