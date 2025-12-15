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
 * Describes how a layout element grows or shrinks relative to available space.
 */
public class Constraint {

  private static final Constraint IMMUTABLE_RIGID_INSTANCE = new Constraint();
  private static final Constraint IMMUTABLE_GROW_INSTANCE = new Constraint(0.5D, 0.0D);
  private static final Constraint IMMUTABLE_SHRINK_INSTANCE = new Constraint(0.0D, 0.5D);
  private static final Constraint IMMUTABLE_GROW_AND_SHRINK_INSTANCE = new Constraint(0.5D, 0.5D);

  private double grow;
  private double shrink;

  /**
   * Creates a constraint with zero grow and shrink factors.
   */
  public Constraint () {

  }

  /**
   * Creates a constraint with explicit grow and shrink weights.
   *
   * @param grow   proportion used when extra space is available
   * @param shrink proportion used when space is scarce
   */
  public Constraint (double grow, double shrink) {

    this.grow = grow;
    this.shrink = shrink;
  }

  /**
   * Returns a reusable constraint that is rigid (no grow or shrink).
   *
   * @return an immutable constraint
   */
  public static Constraint immutable () {

    return IMMUTABLE_RIGID_INSTANCE;
  }

  /**
   * Returns a reusable constraint that only grows.
   *
   * @return an immutable grow-only constraint
   */
  public static Constraint expand () {

    return IMMUTABLE_GROW_INSTANCE;
  }

  /**
   * Returns a reusable constraint that only shrinks.
   *
   * @return an immutable shrink-only constraint
   */
  public static Constraint contract () {

    return IMMUTABLE_SHRINK_INSTANCE;
  }

  /**
   * Returns a reusable constraint that can both grow and shrink.
   *
   * @return an immutable stretch constraint
   */
  public static Constraint stretch () {

    return IMMUTABLE_GROW_AND_SHRINK_INSTANCE;
  }

  /**
   * Creates a mutable constraint instance for custom settings.
   *
   * @return a new mutable constraint
   */
  public static MutableConstraint create () {

    return new MutableConstraint();
  }

  /**
   * Returns the grow weight.
   *
   * @return the grow factor
   */
  public double getGrow () {

    return grow;
  }

  /**
   * Sets the grow weight.
   *
   * @param grow the grow factor
   */
  private void setGrow (double grow) {

    this.grow = grow;
  }

  /**
   * Returns the shrink weight.
   *
   * @return the shrink factor
   */
  public double getShrink () {

    return shrink;
  }

  /**
   * Sets the shrink weight.
   *
   * @param shrink the shrink factor
   */
  private void setShrink (double shrink) {

    this.shrink = shrink;
  }
}
