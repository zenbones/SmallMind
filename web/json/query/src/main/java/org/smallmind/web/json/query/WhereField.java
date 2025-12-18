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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Leaf criterion representing a field comparison against an operand using an operator.
 */
@XmlRootElement(name = "field", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereCriterionPolymorphicXmlAdapter.class)
public class WhereField extends WhereCriterion {

  private WhereOperand<?> operand;
  private WhereOperator operator;
  private String entity;
  private String name;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public WhereField () {

  }

  /**
   * Creates a field criterion within the default entity context.
   *
   * @param name     field name
   * @param operator comparison operator
   * @param operand  operand value
   */
  public WhereField (String name, WhereOperator operator, WhereOperand operand) {

    this.name = name;
    this.operator = operator;
    this.operand = operand;
  }

  /**
   * Creates a field criterion within a specific entity alias.
   *
   * @param entity   entity alias
   * @param name     field name
   * @param operator comparison operator
   * @param operand  operand value
   */
  public WhereField (String entity, String name, WhereOperator operator, WhereOperand operand) {

    this(name, operator, operand);
    this.entity = entity;
  }

  /**
   * Convenience factory for a field in the default entity.
   *
   * @param name     field name
   * @param operator comparison operator
   * @param operand  operand value
   * @return configured field criterion
   */
  public static WhereField instance (String name, WhereOperator operator, WhereOperand operand) {

    return new WhereField(name, operator, operand);
  }

  /**
   * Convenience factory for a field in a specific entity.
   *
   * @param entity   entity alias
   * @param name     field name
   * @param operator comparison operator
   * @param operand  operand value
   * @return configured field criterion
   */
  public static WhereField instance (String entity, String name, WhereOperator operator, WhereOperand operand) {

    return new WhereField(entity, name, operator, operand);
  }

  /**
   * Identifies this criterion as a field comparison.
   *
   * @return {@link CriterionType#FIELD}
   */
  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.FIELD;
  }

  /**
   * Returns the entity alias, if any, associated with the field.
   *
   * @return entity alias or {@code null}
   */
  @XmlElement(name = "entity")
  public String getEntity () {

    return entity;
  }

  /**
   * Sets the entity alias associated with the field.
   *
   * @param entity entity alias
   */
  public void setEntity (String entity) {

    this.entity = entity;
  }

  /**
   * Returns the field name.
   *
   * @return field name
   */
  @XmlElement(name = "name", required = true)
  public String getName () {

    return name;
  }

  /**
   * Sets the field name.
   *
   * @param name field name
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Returns the operand used in the comparison.
   *
   * @return operand value
   */
  @XmlElement(name = "operand")
  public WhereOperand<?> getOperand () {

    return operand;
  }

  /**
   * Sets the operand used in the comparison.
   *
   * @param operand operand value
   */
  public void setOperand (WhereOperand<?> operand) {

    this.operand = operand;
  }

  /**
   * Returns the operator applied to the field.
   *
   * @return comparison operator
   */
  @XmlElement(name = "operator", required = true)
  @XmlJavaTypeAdapter(WhereOperatorEnumXmlAdapter.class)
  public WhereOperator getOperator () {

    return operator;
  }

  /**
   * Sets the operator applied to the field.
   *
   * @param operator comparison operator
   */
  public void setOperator (WhereOperator operator) {

    this.operator = operator;
  }
}
