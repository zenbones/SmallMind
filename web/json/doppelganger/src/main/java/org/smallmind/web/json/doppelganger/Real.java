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
package org.smallmind.web.json.doppelganger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Real {

  // the list of alternate idioms in which this property should be included (overrides the default idiom)
  Idiom[] idioms () default {};

  // the xml adapter to be used for this property
  Class<? extends XmlAdapter> adapter () default NullXmlAdapter.class;

  // a type hint for tools which may process generated classes
  Class<?> as () default Void.class;

  // the type information for the referenced property
  Type type ();

  // the field name of the referenced property
  String field ();

  // the xml element name
  String name () default "";

  // if the xml element is required, may be overridden by an idiom if false
  boolean required () default false;

  // The text for a generated @Comment annotation
  String comment () default "";
}
