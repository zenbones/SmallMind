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

import java.util.HashMap;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class QJoins extends HashMap<EntityPath<?>, BooleanExpression> {

  private static final QJoins EMPTY_JOINS = new QJoins();

  private QJoin[] series;
  private int lowestIndex;

  public QJoins (QJoin... series) {

    this.series = series;

    lowestIndex = (series == null) ? 0 : series.length;
  }

  public static QJoins empty () {

    return EMPTY_JOINS;
  }

  public void use (EntityPath<?> root) {

    if (lowestIndex > 0) {
      for (int index = 0; index < series.length; index++) {
        if (series[index].getRoot().equals(root)) {
          if (index < lowestIndex) {
            lowestIndex = index;
          }
          break;
        }
      }
    }
  }

  public void update (JPAQuery<?> query) {

    if (series != null) {
      for (int index = lowestIndex; index < series.length; index++) {
        switch (series[index].getType()) {
          case INNER:
            query.from(series[index].getRoot()).where(series[index].getPredicate());
            // query.leftJoin(series[index].getRoot()).on(series[index].getPredicate());
            break;
          case LEFT:
            query.leftJoin(series[index].getRoot()).on(series[index].getPredicate());
            break;
          case RIGHT:
            query.rightJoin(series[index].getRoot()).on(series[index].getPredicate());
            break;
          default:
            throw new UnknownSwitchCaseException(series[index].getType().name());
        }
      }
    }
  }
}
