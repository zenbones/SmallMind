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
import java.util.HashSet;
import java.util.LinkedList;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.query.JoinType;

public class QJoins extends HashMap<EntityPath<?>, BooleanExpression> {

  private HashMap<EntityPath<?>, Instruction> instructionMap = new HashMap<>();
  private HashSet<EntityPath<?>> encounteredSet = new HashSet<>();
  private LinkedList<EntityPath<?>> hierarchy = new LinkedList<>();

  public QJoins independent (QJoin... series) {

    for (QJoin join : series) {
      instructionMap.putIfAbsent(join.getRoot(), new Instruction(join.getType(), join.getPredicate()));
    }

    return this;
  }

  public QJoins hierarchical (QJoin... series) {

    for (QJoin join : series) {
      if (instructionMap.putIfAbsent(join.getRoot(), new Instruction(join.getType(), join.getPredicate())) == null) {
        hierarchy.add(join.getRoot());
      }
    }

    return this;
  }

  public void use (EntityPath<?> root) {

    encounteredSet.add(root);
  }

  public void update (JPAQuery<?> query) {

    if (!hierarchy.isEmpty()) {

      boolean active = false;

      for (EntityPath<?> element : hierarchy) {
        if (encounteredSet.remove(element)) {
          active = true;
        }
        if (active) {
          execute(query, element, instructionMap.get(element));
        }
      }
    }

    for (EntityPath<?> root : encounteredSet) {
      execute(query, root, instructionMap.get(root));
    }
  }

  private void execute (JPAQuery<?> query, EntityPath<?> root, Instruction instruction) {

    switch (instruction.getType()) {
      case INNER:
        query.from(root).where(instruction.getPredicate());
        // query.leftJoin(instruction.getRoot()).on(instruction.getPredicate());
        break;
      case LEFT:
        query.leftJoin(root).on(instruction.getPredicate());
        break;
      case RIGHT:
        query.rightJoin(root).on(instruction.getPredicate());
        break;
      default:
        throw new UnknownSwitchCaseException(instruction.getType().name());
    }
  }

  private class Instruction {

    private JoinType type;
    private Predicate predicate;

    private Instruction (JoinType type, Predicate predicate) {

      this.type = type;
      this.predicate = predicate;
    }

    private JoinType getType () {

      return type;
    }

    private Predicate getPredicate () {

      return predicate;
    }
  }
}
