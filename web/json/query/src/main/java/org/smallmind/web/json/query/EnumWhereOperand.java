/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "enum")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class EnumWhereOperand<E extends Enum<E>> implements WhereOperand<E> {

  private E value;
  private String hint;

  public EnumWhereOperand () {

  }

  public EnumWhereOperand (String hint, E value) {

    this.hint = hint;
    this.value = value;
  }

  public static <E extends Enum<E>> EnumWhereOperand instance (String typeHint, E enumeration) {

    return new EnumWhereOperand<>(typeHint, enumeration);
  }

  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ENUM;
  }

  @XmlElement(name = "hint", required = true)
  public String getHint () {

    return hint;
  }

  public void setHint (String hint) {

    this.hint = hint;
  }

  @XmlElement(name = "value", required = true)
  public E getValue () {

    return value;
  }

  public void setValue (E value) {

    this.value = value;
  }
}