/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "field", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereCriterionPolymorphicXmlAdapter.class)
public class WhereField extends WhereCriterion {

  private WhereOperand<?> operand;
  private WhereOperator operator;
  private String entity;
  private String name;

  public WhereField () {

  }

  public WhereField (String name, WhereOperator operator, WhereOperand operand) {

    this.name = name;
    this.operator = operator;
    this.operand = operand;
  }

  public WhereField (String entity, String name, WhereOperator operator, WhereOperand operand) {

    this(name, operator, operand);
    this.entity = entity;
  }

  public static WhereField instance (String name, WhereOperator operator, WhereOperand operand) {

    return new WhereField(name, operator, operand);
  }

  public static WhereField instance (String entity, String name, WhereOperator operator, WhereOperand operand) {

    return new WhereField(entity, name, operator, operand);
  }

  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.FIELD;
  }

  @XmlElement(name = "entity")
  public String getEntity () {

    return entity;
  }

  public void setEntity (String entity) {

    this.entity = entity;
  }

  @XmlElement(name = "name", required = true)
  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  @XmlElement(name = "operand")
  public WhereOperand<?> getOperand () {

    return operand;
  }

  public void setOperand (WhereOperand<?> operand) {

    this.operand = operand;
  }

  @XmlElement(name = "operator", required = true)
  @XmlJavaTypeAdapter(WhereOperatorEnumXmlAdapter.class)
  public WhereOperator getOperator () {

    return operator;
  }

  public void setOperator (WhereOperator operator) {

    this.operator = operator;
  }
}
