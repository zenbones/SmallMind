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
package org.smallmind.web.json.doppelganger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain class for Doppelganger processing. The processor generates DTO-style JAXB/Jackson compatible
 * views for inbound and outbound directions according to the declared virtual/real properties, idioms,
 * polymorphism/hierarchy, and supplemental metadata.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Doppelganger {

  /**
   * @return the list of virtual properties (not backed by real fields) to inject into generated views
   */
  // the list of virtual properties to be added to the generated class
  Virtual[] virtual () default {};

  /**
   * @return the list of actual class fields that should be exposed with alternate metadata
   */
  // the list of real properties to be added to the generated class
  Real[] real () default {};

  /**
   * @return the set of pledges that force generation of specific purpose/visibility views even when empty
   */
  // the list of conditions under which to guarantee a class is generated (should be used only when the class would otherwise not be generated)
  Pledge[] pledges () default {};

  /**
   * @return polymorphic handling details for the annotated class
   */
  // the requirements for polymorphic annotations
  Polymorphic polymorphic () default @Polymorphic();

  /**
   * @return inheritance hierarchy handling details for the annotated class
   */
  // the requirements for hierarchy annotations
  Hierarchy hierarchy () default @Hierarchy();

  /**
   * @return idiom-level constraints to apply at the class level for selected purposes/directions
   */
  // the constraint annotations to be applied to the generated class
  Idiom[] constrainingIdioms () default {};

  /**
   * @return additional interfaces the generated view classes should implement
   */
  // Additional interfaces that the generated class should be marked as implementing
  Implementation[] implementations () default {};

  /**
   * @return additional imports to be injected into generated view source
   */
  // Additional imports to be added to the generated class
  Import[] imports () default {};

  /**
   * @return explicit XML element name for the generated root (defaults to decapitalized class name)
   */
  // the xml root element name
  String name () default "";

  /**
   * @return XML namespace for the generated root element
   */
  String namespace () default "http://org.smallmind/web/json/doppelganger";

  /**
   * @return whether generated views should implement {@link java.io.Serializable}
   */
  boolean serializable () default false;

  /**
   * @return text to include in a generated {@code @Comment} annotation for every view
   */
  // The text for a generated @Comment annotation
  String comment () default "";
}
