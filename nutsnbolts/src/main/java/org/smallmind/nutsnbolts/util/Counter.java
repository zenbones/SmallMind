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
package org.smallmind.nutsnbolts.util;

/**
 * Simple mutable integer counter with increment/decrement helpers.
 */
public class Counter {

  private int count;

  /**
   * Initializes the counter to zero.
   */
  public Counter () {

    count = 0;
  }

  /**
   * Initializes the counter to the provided value.
   *
   * @param count starting value
   */
  public Counter (int count) {

    this.count = count;
  }

  /**
   * Adds the delta and returns the updated value.
   *
   * @param delta amount to add
   * @return new counter value
   */
  public int addAndGet (int delta) {

    count += delta;

    return count;
  }

  /**
   * Returns the current value, then adds the delta.
   *
   * @param delta amount to add
   * @return value before increment
   */
  public int getAndAdd (int delta) {

    int current = count;

    count += delta;

    return current;
  }

  /**
   * Subtracts the delta and returns the updated value.
   *
   * @param delta amount to subtract
   * @return new counter value
   */
  public int subtractAndGet (int delta) {

    count -= delta;

    return count;
  }

  /**
   * Returns the current value, then subtracts the delta.
   *
   * @param delta amount to subtract
   * @return value before decrement
   */
  public int getAndSubtract (int delta) {

    int current = count;

    count -= delta;

    return current;
  }

  /**
   * Increments and returns the updated value.
   *
   * @return incremented value
   */
  public int incAndGet () {

    return ++count;
  }

  /**
   * Returns the current value, then increments.
   *
   * @return value before increment
   */
  public int getAndInc () {

    return count++;
  }

  /**
   * Decrements and returns the updated value.
   *
   * @return decremented value
   */
  public int decAndGet () {

    return --count;
  }

  /**
   * Returns the current value, then decrements.
   *
   * @return value before decrement
   */
  public int getAndDec () {

    return count--;
  }

  /**
   * @return current counter value
   */
  public int get () {

    return count;
  }
}
