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
 * Hint describing the component type of an array operand.
 */
@XmlRootElement(name = "component", namespace = "http://org.smallmind/web/json/query")
@XmlJavaTypeAdapter(HintPolymorphicXmlAdapter.class)
public class ComponentHint extends Hint {

  private ComponentType type;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public ComponentHint () {

  }

  /**
   * Creates a component hint for the supplied type.
   *
   * @param type component type
   */
  public ComponentHint (ComponentType type) {

    this.type = type;
  }

  /**
   * @return {@link HintType#COMPONENT}
   */
  @Override
  @XmlTransient
  public HintType getHintType () {

    return HintType.COMPONENT;
  }

  /**
   * Returns the component type represented by this hint.
   *
   * @return component type
   */
  @XmlElement(name = "type", required = true)
  @XmlJavaTypeAdapter(ComponentTypeEnumXmlAdapter.class)
  public ComponentType getType () {

    return type;
  }

  /**
   * Sets the component type represented by this hint.
   *
   * @param type component type
   */
  public void setType (ComponentType type) {

    this.type = type;
  }
}
