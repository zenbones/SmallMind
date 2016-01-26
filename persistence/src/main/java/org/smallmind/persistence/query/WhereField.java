/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.query;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "field")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class WhereField implements WhereCriterion {

  private WhereValue value;
  private WhereOperation operation;
  private String name;

  public WhereField () {

  }

  public WhereField (String name, WhereOperation operation, WhereValue value) {

    this.name = name;
    this.operation = operation;
    this.value = value;
  }

  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.FIELD;
  }

  @XmlElement(name = "name", required = true, nillable = false)
  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  @XmlElementRefs({@XmlElementRef(type = BooleanWhereValue.class), @XmlElementRef(type = ByteWhereValue.class), @XmlElementRef(type = CharacterWhereValue.class), @XmlElementRef(type = DateWhereValue.class),
                    @XmlElementRef(type = DoubleWhereValue.class), @XmlElementRef(type = EnumWhereValue.class), @XmlElementRef(type = FloatWhereValue.class), @XmlElementRef(type = IntegerWhereValue.class),
                    @XmlElementRef(type = LongWhereValue.class), @XmlElementRef(type = ShortWhereValue.class), @XmlElementRef(type = StringWhereValue.class)})
  public WhereValue getValue () {

    return value;
  }

  public void setValue (WhereValue value) {

    this.value = value;
  }

  @XmlElement(name = "operation", required = true, nillable = false)
  @XmlJavaTypeAdapter(WhereOperationEnumXmlAdapter.class)
  public WhereOperation getOperation () {

    return operation;
  }

  public void setOperation (WhereOperation operation) {

    this.operation = operation;
  }

  @Override
  public int hashCode () {

    return name.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof WhereField) && ((WhereField)obj).getName().equals(name) && ((WhereField)obj).getOperation().equals(operation) && ((WhereField)obj).getValue().getValue().equals(value.getValue());
  }
}