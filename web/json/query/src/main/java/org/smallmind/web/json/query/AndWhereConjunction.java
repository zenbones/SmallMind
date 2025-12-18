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

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Logical AND grouping of multiple {@link WhereCriterion} elements.
 */
@XmlRootElement(name = "and", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereCriterionPolymorphicXmlAdapter.class)
public class AndWhereConjunction extends WhereConjunction {

  /**
   * Creates an empty conjunction.
   */
  public AndWhereConjunction () {

  }

  /**
   * Creates a conjunction containing the supplied criteria.
   *
   * @param criteria the nested criteria to be combined with AND
   */
  public AndWhereConjunction (WhereCriterion... criteria) {

    super(criteria);
  }

  /**
   * Convenience factory for an AND conjunction.
   *
   * @param criteria the nested criteria to be combined with AND
   * @return a new {@link AndWhereConjunction} containing the criteria
   */
  public static AndWhereConjunction instance (WhereCriterion... criteria) {

    return new AndWhereConjunction(criteria);
  }

  /**
   * Identifies this conjunction as an AND operation.
   *
   * @return {@link ConjunctionType#AND}
   */
  @Override
  @XmlTransient
  public ConjunctionType getConjunctionType () {

    return ConjunctionType.AND;
  }
}
