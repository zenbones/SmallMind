/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.orm.querydsl;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathBuilder;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.query.WhereFieldTransformer;

public class QWhereFieldTransformer extends WhereFieldTransformer<EntityPath<?>, Path<?>> {

  public QWhereFieldTransformer (EntityPath<? extends Durable<?>> entityPath) {

    super((String entity, String name) -> new QWherePath(entityPath, new PathBuilder<>(entityPath.getType(), entityPath.toString()).get(name), name));
  }

  public QWhereFieldTransformer (EntityPath<? extends Durable<?>> entityPath, UnaryOperator<String> nameOperator) {

    super((String entity, String name) -> {

      String transformedName = nameOperator.apply(name);

      return new QWherePath(entityPath, new PathBuilder<>(entityPath.getType(), entityPath.toString()).get(transformedName), transformedName);
    });
  }

  public QWhereFieldTransformer (Function<String, EntityPath<? extends Durable<?>>> pathFunction, UnaryOperator<String> nameFunction) {

    super((String entity, String name) -> {

      EntityPath<? extends Durable<?>> transformedEntityPath = pathFunction.apply(name);
      String transformedName = nameFunction.apply(name);

      return new QWherePath(transformedEntityPath, new PathBuilder<>(transformedEntityPath.getType(), transformedEntityPath.toString()).get(transformedName), transformedName);
    });
  }

  public QWhereFieldTransformer (Function<String, Path<? extends Durable<?>>> pathFunction) {

    super((String entity, String name) -> {

      Path<? extends Durable<?>> transformedPath = pathFunction.apply(name);

      return new QWherePath((EntityPath<?>)transformedPath.getRoot(), transformedPath, transformedPath.toString().substring(transformedPath.getRoot().toString().length() + 1));
    });
  }
}
