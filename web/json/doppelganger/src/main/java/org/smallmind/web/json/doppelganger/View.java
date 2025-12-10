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
package org.smallmind.web.json.doppelganger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Marks a field or accessor to be included in generated views, allowing idiom-specific overrides.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface View {

  /**
   * @return idioms that override the default inclusion rules for this property
   */
  // the list of alternate idioms in which this property should be included (overrides the default idiom)
  Idiom[] idioms () default {};

  /**
   * @return JAXB adapter to apply on the generated view property
   */
  // the xml adapter to be used for this property
  Class<? extends XmlAdapter> adapter () default NullXmlAdapter.class;

  /**
   * @return type hint for tools (mapped to {@link org.smallmind.web.json.scaffold.util.As})
   */
  // a type hint for tools which may process generated classes
  Class<?> as () default Void.class;

  /**
   * @return XML element name override for this property
   */
  // the xml element name
  String name () default "";

  /**
   * @return whether the property is required in the default idiom
   */
  // if the xml element is required, may be overridden by an idiom if false
  boolean required () default false;

  /**
   * @return comment text to attach to the generated property
   */
  // The text for a generated @Comment annotation
  String comment () default "";
}
