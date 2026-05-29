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
 * A fluent, mutable subtype of {@link Constraint} that allows grow and shrink weights to be
 * configured via chained setter calls; obtain an instance via {@link Constraint#create()}.
 */
public class MutableConstraint extends Constraint {

  /**
   * Constructs a mutable constraint with grow and shrink weights of zero.
   */
  protected MutableConstraint () {

  }

  /**
   * Sets the grow weight to {@code 0.5}, enabling this element to accept surplus space.
   *
   * @return this constraint for method chaining
   */
  public MutableConstraint mayGrow () {

    return setGrow(0.5D);
  }

  /**
   * Sets the grow weight to the given value.
   *
   * @param grow the grow factor; must be non-negative
   * @return this constraint for method chaining
   */
  public MutableConstraint setGrow (double grow) {

    this.grow = grow;

    return this;
  }

  /**
   * Sets the shrink weight to {@code 0.5}, enabling this element to yield space under contraction.
   *
   * @return this constraint for method chaining
   */
  public MutableConstraint mayShrink () {

    return setShrink(0.5D);
  }

  /**
   * Sets the shrink weight to the given value.
   *
   * @param shrink the shrink factor; must be non-negative
   * @return this constraint for method chaining
   */
  public MutableConstraint setShrink (double shrink) {

    this.shrink = shrink;

    return this;
  }
}
