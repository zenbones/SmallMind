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
package org.smallmind.memcached.cubby.locator;

/**
 * Utility class providing prime-number generation support for hash-based routing algorithms.
 *
 * <p>The Maglev consistent-hashing algorithm ({@link MaglevKeyLocator}) requires a prime-sized
 * lookup table to guarantee that every slot is filled during table construction. This class
 * supplies the {@link #nextPrime(int)} method used to compute that table size from the number
 * of hosts and the configured virtual host count.</p>
 *
 * <p>All methods are static; this class is not intended to be instantiated.</p>
 */
public class PrimeGenerator {

  /**
   * Tests whether the given integer is a prime number.
   *
   * <p>Uses trial division with the standard 6k ± 1 optimisation: after handling the special
   * cases for values less than or equal to 3 and for multiples of 2 or 3, candidate divisors
   * are tested at {@code 5, 7, 11, 13, ...} up to {@code sqrt(n)}.</p>
   *
   * @param n the integer to test; must be a non-negative value
   * @return {@code true} if {@code n} is prime; {@code false} otherwise
   */
  private static boolean isPrime (int n) {

    if (n <= 1) {

      return false;
    } else if (n <= 3) {

      return true;
    } else {
      if ((n % 2 == 0) || (n % 3 == 0)) {

        return false;
      } else {
        for (int i = 5; i * i <= n; i = i + 6) {
          if ((n % i == 0) || (n % (i + 2) == 0)) {

            return false;
          }
        }

        return true;
      }
    }
  }

  /**
   * Returns the smallest prime number that is strictly greater than {@code n}.
   *
   * <p>If {@code n} is less than or equal to 1 the method immediately returns 2, the smallest
   * prime. Otherwise candidates are tested in ascending order starting at {@code n + 1}.</p>
   *
   * @param n the lower bound; the returned prime will be strictly greater than this value
   * @return the next prime number greater than {@code n}
   */
  public static int nextPrime (int n) {

    if (n <= 1) {

      return 2;
    } else {

      int prime = n;

      while (true) {
        if (isPrime(++prime)) {

          return prime;
        }
      }
    }
  }
}
