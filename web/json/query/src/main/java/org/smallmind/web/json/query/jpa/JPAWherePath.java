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
package org.smallmind.web.json.query.jpa;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.smallmind.web.json.query.WherePath;

/**
 * JPA Criteria implementation of {@link WherePath}, capturing root and path for a field.
 */
public class JPAWherePath extends WherePath<Root<?>, Path<?>> {

  private final Root<?> root;
  private final Path<?> path;
  private final String field;

  /**
   * Builds a path from a root and field name by calling {@link Root#get(String)}.
   *
   * @param root  JPA root
   * @param field field name
   */
  public JPAWherePath (Root<?> root, String field) {

    this.root = root;
    this.field = field;

    path = root.get(field);
  }

  /**
   * @return JPA root
   */
  @Override
  public Root<?> getRoot () {

    return root;
  }

  /**
   * @return JPA path to the field
   */
  @Override
  public Path<?> getPath () {

    return path;
  }

  /**
   * @return name of the terminal field
   */
  @Override
  public String getField () {

    return field;
  }
}
