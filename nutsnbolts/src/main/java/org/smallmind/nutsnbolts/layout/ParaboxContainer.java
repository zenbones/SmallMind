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
package org.smallmind.nutsnbolts.layout;

/**
 * Abstraction for platform-specific containers that can host components laid out by {@link ParaboxLayout}.
 *
 * @param <C> the component type used by the platform
 */
public interface ParaboxContainer<C> {

  /**
   * Returns the platform abstraction providing gap and metric data.
   *
   * @return the platform
   */
  ParaboxPlatform getPlatform ();

  /**
   * Wraps a platform component in a {@link ParaboxElement} using the provided constraint.
   *
   * @param component  the platform component
   * @param constraint sizing and alignment constraint
   * @return a new element wrapper
   */
  ParaboxElement<C> constructElement (C component, Constraint constraint);

  /**
   * Adds a component to the native container.
   *
   * @param component the component to add
   */
  void nativelyAddComponent (C component);

  /**
   * Removes a component from the native container.
   *
   * @param component the component to remove
   */
  void nativelyRemoveComponent (C component);
}
