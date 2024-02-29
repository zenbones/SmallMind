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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(WhereCriterionPolymorphicXmlAdapter.class)
public abstract class WhereConjunction extends WhereCriterion {

  private List<WhereCriterion> criterionList;

  public WhereConjunction () {

  }

  public WhereConjunction (WhereCriterion... criteria) {

    criterionList = new LinkedList<>();

    for (WhereCriterion criterion : criteria) {
      if (criterion != null) {
        criterionList.add(criterion);
      }
    }
  }

  public abstract ConjunctionType getConjunctionType ();

  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.CONJUNCTION;
  }

  @XmlTransient
  public synchronized boolean isEmpty () {

    return (criterionList == null) || criterionList.isEmpty();
  }

  @XmlTransient
  public synchronized int size () {

    return (criterionList == null) ? 0 : criterionList.size();
  }

  @XmlElement(name = "criteria")
  public synchronized WhereCriterion[] getCriteria () {

    WhereCriterion[] criteria = new WhereCriterion[criterionList == null ? 0 : criterionList.size()];

    if (criterionList != null) {
      criterionList.toArray(criteria);
    }

    return criteria;
  }

  public synchronized void setCriteria (WhereCriterion... criteria) {

    this.criterionList = Arrays.asList(criteria);
  }

  public synchronized void addCriterion (WhereCriterion criterion) {

    if (criterionList == null) {
      criterionList = new LinkedList<>();
    }

    criterionList.add(criterion);
  }
}
