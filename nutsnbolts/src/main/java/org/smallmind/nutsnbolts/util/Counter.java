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
 * A simple non-thread-safe mutable integer counter with arithmetic and increment/decrement convenience methods.
 */
public class Counter {

  private int count;

  /**
   * Constructs a counter initialized to zero.
   */
  public Counter () {

    count = 0;
  }

  /**
   * Constructs a counter initialized to the given value.
   *
   * @param count the starting value
   */
  public Counter (int count) {

    this.count = count;
  }

  /**
   * Adds the given delta to the counter and returns the updated value.
   *
   * @param delta the amount to add
   * @return the counter value after the addition
   */
  public int addAndGet (int delta) {

    count += delta;

    return count;
  }

  /**
   * Returns the current counter value and then adds the given delta.
   *
   * @param delta the amount to add
   * @return the counter value before the addition
   */
  public int getAndAdd (int delta) {

    int current = count;

    count += delta;

    return current;
  }

  /**
   * Subtracts the given delta from the counter and returns the updated value.
   *
   * @param delta the amount to subtract
   * @return the counter value after the subtraction
   */
  public int subtractAndGet (int delta) {

    count -= delta;

    return count;
  }

  /**
   * Returns the current counter value and then subtracts the given delta.
   *
   * @param delta the amount to subtract
   * @return the counter value before the subtraction
   */
  public int getAndSubtract (int delta) {

    int current = count;

    count -= delta;

    return current;
  }

  /**
   * Increments the counter by one and returns the updated value.
   *
   * @return the counter value after incrementing
   */
  public int incAndGet () {

    return ++count;
  }

  /**
   * Returns the current counter value and then increments it by one.
   *
   * @return the counter value before incrementing
   */
  public int getAndInc () {

    return count++;
  }

  /**
   * Decrements the counter by one and returns the updated value.
   *
   * @return the counter value after decrementing
   */
  public int decAndGet () {

    return --count;
  }

  /**
   * Returns the current counter value and then decrements it by one.
   *
   * @return the counter value before decrementing
   */
  public int getAndDec () {

    return count--;
  }

  /**
   * Returns the current counter value without modifying it.
   *
   * @return the current counter value
   */
  public int get () {

    return count;
  }
}
