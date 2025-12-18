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
package org.smallmind.web.json.query.querydsl;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathBuilder;
import org.smallmind.persistence.Durable;
import org.smallmind.web.json.query.WherePath;

/**
 * QueryDSL implementation of {@link WherePath}, capturing the root and field path for a predicate.
 */
public class QueryDslWherePath extends WherePath<Path<?>, Path<?>> {

  private final Path<?> root;
  private final Path<?> path;
  private final String field;

  /**
   * Derives the field name from an existing path and uses its root.
   *
   * @param path querydsl path
   */
  public QueryDslWherePath (Path<?> path) {

    this(path.getRoot(), path, path.toString().substring(path.getRoot().toString().length() + 1));
  }

  /**
   * Constructs a path by appending the given field to the provided durable root path.
   *
   * @param path  root durable path
   * @param field field name to append
   */
  public QueryDslWherePath (Path<? extends Durable<?>> path, String field) {

    this(path, new PathBuilder<>(path.getType(), path.toString()).get(field), field);
  }

  /**
   * Constructs a path with explicit root, path, and field components.
   *
   * @param root  root path
   * @param path  full path
   * @param field field name
   */
  public QueryDslWherePath (Path<?> root, Path<?> path, String field) {

    this.root = root;
    this.path = path;
    this.field = field;
  }

  /**
   * @return querydsl root path
   */
  @Override
  public Path<?> getRoot () {

    return root;
  }

  /**
   * @return full querydsl path
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
