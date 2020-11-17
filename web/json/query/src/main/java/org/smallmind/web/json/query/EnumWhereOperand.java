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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "enum", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(WhereOperandPolymorphicXmlAdapter.class)
public class EnumWhereOperand<E extends Enum<E>> extends WhereOperand<E> {

  private EnumHint hint;
  private String value;

  public EnumWhereOperand () {

  }

  public EnumWhereOperand (E enumeration) {

    hint = new EnumHint(enumeration.getClass());
    this.value = enumeration.name();
  }

  public static <E extends Enum<E>> EnumWhereOperand instance (E enumeration) {

    return new EnumWhereOperand<E>(enumeration);
  }

  @Override
  @XmlTransient
  public ElementType getElementType () {

    return ElementType.STRING;
  }

  @Override
  @XmlTransient
  public OperandType getOperandType () {

    return OperandType.ENUM;
  }

  @Override
  @XmlTransient
  public E get () {

    try {

      return Enum.valueOf((Class<E>)Class.forName(hint.getType()), value);
    } catch (ClassNotFoundException classNotFoundException) {
      throw new QueryProcessingException(classNotFoundException);
    }
  }

  @XmlElement(name = "hint", required = true)
  public EnumHint getHint () {

    return hint;
  }

  public void setHint (EnumHint hint) {

    this.hint = hint;
  }

  @XmlElement(name = "value", required = true)
  public String getValue () {

    return value;
  }

  public void setValue (String value) {

    this.value = value;
  }
}