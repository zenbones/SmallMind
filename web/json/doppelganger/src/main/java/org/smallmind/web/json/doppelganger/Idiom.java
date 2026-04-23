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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a named purpose/visibility context that controls which generated views include an annotated property
 * and what constraints and requirements apply within that context.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Idiom {

  /**
   * @return constraints to apply to the property or class when this idiom is active
   */
  // the constraint annotations to be applied to the property within this idiom
  Constraint[] constraints () default {};

  /**
   * @return visibility (IN, OUT, or BOTH) controlling which view directions this idiom applies to
   */
  // the visibility of the property within this idiom (IN, OUT or BOTH)
  Visibility visibility () default Visibility.BOTH;

  /**
   * @return purpose identifiers (e.g. {@code "create"}, {@code "update"}) that select the views affected by this idiom
   */
  // the name(s) of this idiom (a short descriptive string such as 'create' or 'internal')
  String[] purposes () default {};

  /**
   * @return whether the property is required when this idiom is active (may also be implied by a {@code @NotNull} constraint)
   */
  // if the xml element is required in this idiom, if false may overridden by use of a NotNull constraint
  boolean required () default false;
}
