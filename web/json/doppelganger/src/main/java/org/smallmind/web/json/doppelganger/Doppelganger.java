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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Doppelganger {

  // the list of virtual properties to be added to the generated class
  Virtual[] virtual () default {};

  // the list of real properties to be added to the generated class
  Real[] real () default {};

  // the list of conditions under which to guarantee a class is generated (should be used only when the class would otherwise not be generated)
  Pledge[] pledges () default {};

  // the requirements for polymorphic annotations
  Polymorphic polymorphic () default @Polymorphic();

  // the constraint annotations to be applied to the generated class
  Constraint[] constraints () default {};

  // Additional interfaces that the generated class should be marked as implementing
  Implementation[] implementations () default {};

  // Additional imports to be added to the generated class
  Import[] imports () default {};

  // the xml root element name
  String name () default "";

  String namespace () default "http://org.smallmind/web/json/doppelganger";

  boolean serializable () default false;

  // The text for a generated @Comment annotation
  String comment () default "";
}