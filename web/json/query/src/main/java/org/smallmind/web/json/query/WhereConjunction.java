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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Abstract logical conjunction that groups child {@link WhereCriterion} elements (e.g., AND/OR).
 */
@XmlJavaTypeAdapter(WhereCriterionPolymorphicXmlAdapter.class)
public abstract class WhereConjunction extends WhereCriterion {

  private List<WhereCriterion> criterionList;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public WhereConjunction () {

  }

  /**
   * Builds a conjunction initialized with the provided criteria, omitting any {@code null} entries.
   *
   * @param criteria child criteria to combine
   */
  public WhereConjunction (WhereCriterion... criteria) {

    if ((criteria != null) && (criteria.length > 0)) {

      criterionList = new LinkedList<>();

      for (WhereCriterion criterion : criteria) {
        if (criterion != null) {
          criterionList.add(criterion);
        }
      }
    }
  }

  /**
   * Returns the specific conjunction type (AND/OR).
   *
   * @return the conjunction type
   */
  public abstract ConjunctionType getConjunctionType ();

  /**
   * Identifies this criterion as a conjunction.
   *
   * @return {@link CriterionType#CONJUNCTION}
   */
  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.CONJUNCTION;
  }

  /**
   * Indicates whether the conjunction currently contains any child criteria.
   *
   * @return {@code true} if no child criteria are present
   */
  @XmlTransient
  public synchronized boolean isEmpty () {

    return (criterionList == null) || criterionList.isEmpty();
  }

  /**
   * Returns the number of child criteria contained in this conjunction.
   *
   * @return child criterion count
   */
  @XmlTransient
  public synchronized int size () {

    return (criterionList == null) ? 0 : criterionList.size();
  }

  /**
   * Returns the child criteria as an array in insertion order.
   *
   * @return array of child criteria (never {@code null})
   */
  @XmlElement(name = "criteria")
  public synchronized WhereCriterion[] getCriteria () {

    WhereCriterion[] criteria = new WhereCriterion[criterionList == null ? 0 : criterionList.size()];

    if (criterionList != null) {
      criterionList.toArray(criteria);
    }

    return criteria;
  }

  /**
   * Replaces the child criteria with the provided array.
   *
   * @param criteria new child criteria to set
   */
  public synchronized void setCriteria (WhereCriterion... criteria) {

    this.criterionList = Arrays.asList(criteria);
  }

  /**
   * Adds another child criterion to this conjunction.
   *
   * @param criterion criterion to append
   */
  public synchronized void addCriterion (WhereCriterion criterion) {

    if (criterionList == null) {
      criterionList = new LinkedList<>();
    }

    criterionList.add(criterion);
  }
}
