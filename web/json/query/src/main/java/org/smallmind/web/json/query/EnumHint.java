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
 * Hint capturing the fully qualified class name of an enum type.
 */
@XmlRootElement(name = "enum", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(HintPolymorphicXmlAdapter.class)
public class EnumHint extends Hint {

  private String type;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public EnumHint () {

  }

  /**
   * Creates an enum hint for the supplied enum class.
   *
   * @param enumClass the enum type
   */
  public EnumHint (Class<? extends Enum> enumClass) {

    type = enumClass.getName();
  }

  /**
   * @return {@link HintType#ENUM}
   */
  @Override
  @XmlTransient
  public HintType getHintType () {

    return HintType.ENUM;
  }

  /**
   * Returns the fully qualified enum class name.
   *
   * @return enum class name
   */
  @XmlElement(name = "type")
  public String getType () {

    return type;
  }

  /**
   * Sets the fully qualified enum class name.
   *
   * @param type enum class name
   */
  public void setType (String type) {

    this.type = type;
  }
}
