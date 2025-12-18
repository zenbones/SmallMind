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

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Traversal utilities for walking where clause structures with a {@link WhereVisitor}.
 */
public class WhereUtility {

  /**
   * Traverses the root conjunction of a {@link Where} using the supplied visitor.
   *
   * @param visitor visitor callback
   * @param where   where clause to traverse
   * @throws Exception if the visitor throws or traversal fails
   */
  public static void walk (WhereVisitor visitor, Where where)
    throws Exception {

    walk(visitor, where.getRootConjunction());
  }

  /**
   * Traverses one or more criteria depth-first, invoking the visitor for each node.
   *
   * @param visitor       visitor callback
   * @param whereCriteria criteria to traverse
   * @throws UnknownSwitchCaseException if an unknown criterion type is encountered
   * @throws Exception                  if the visitor throws
   */
  public static void walk (WhereVisitor visitor, WhereCriterion... whereCriteria)
    throws Exception {

    for (WhereCriterion whereCriterion : whereCriteria) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:
          visitor.visitConjunction((WhereConjunction)whereCriterion);
          walk(visitor, ((WhereConjunction)whereCriterion).getCriteria());
          break;
        case FIELD:
          visitor.visitField((WhereField)whereCriterion);
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }
  }
}
