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
 * Utility for encoding non-negative {@code long} values into a custom base-34 alphabet and decoding them back,
 * using a group-specific additive constant to obscure the encoded values.
 */
public class MartianBase34 {

  /**
   * Encoding groups, each carrying a distinct additive mix constant used to offset encoded values.
   */
  public enum Group {

    FIRST(648913), SECOND(247123), THIRD(7294612383675L);

    private final long mixConstant;

    /**
     * Associates the group constant with its additive mix value.
     *
     * @param mixConstant the additive constant applied before encoding to obscure the value
     */
    Group (long mixConstant) {

      this.mixConstant = mixConstant;
    }

    /**
     * Returns the additive mix constant for this group.
     *
     * @return the mix constant
     */
    public long getMixConstant () {

      return mixConstant;
    }
  }

  /**
   * The 34-character alphabet used for encoding, ordered to map digit values 0–33 to their character representation.
   */
  public static final String NUMEROLOGY = "QYEN0MT2PLCW1UF9X8DBZK3A6SR4HVG7J5";

  private static long getMaxMartian (int digits) {

    return (long)Math.pow(34, digits);
  }

  /**
   * Encodes a non-negative base-10 value into the shortest Martian base-34 string (up to 13 digits)
   * that can represent it, applying the given group's mix constant.
   *
   * @param base10 the non-negative decimal value to encode
   * @param group  the encoding group whose mix constant is applied
   * @return the base-34 encoded string using the minimum required number of digits
   * @throws IllegalArgumentException if {@code base10} is negative or exceeds the maximum representable value
   */
  public static String base10To34 (long base10, Group group) {

    if (base10 < 0) {
      throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be >=0");
    }

    for (int digits = 1; digits <= 13; digits++) {
      if (Math.pow(34, digits) >= base10) {
        return base10To34(base10, digits, group);
      }
    }

    throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be no greater than a long value - what language are we speaking here?");
  }

  /**
   * Encodes a non-negative base-10 value into a Martian base-34 string of exactly the specified number of digits,
   * padding with leading zero-character digits as needed and applying the given group's mix constant.
   *
   * @param base10 the non-negative decimal value to encode; must be in the range [0, 34^digits)
   * @param digits the exact number of base-34 output digits to produce
   * @param group  the encoding group whose mix constant is applied
   * @return the base-34 encoded string of the requested length
   * @throws IllegalArgumentException if {@code base10} is out of the range [0, 34^digits)
   */
  public static String base10To34 (long base10, int digits, Group group) {

    StringBuilder martianBuilder = new StringBuilder();
    long maxMartian = getMaxMartian(digits);
    long currentResult;

    if ((base10 < 0) || (base10 > maxMartian)) {
      throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be >=0 and <" + maxMartian);
    }

    currentResult = group.getMixConstant() + base10;
    if (currentResult >= maxMartian) {
      currentResult -= maxMartian;
    }

    while (currentResult != 0) {
      martianBuilder.insert(0, NUMEROLOGY.charAt((int)(currentResult % 34)));
      currentResult /= 34;
    }

    while (martianBuilder.length() < digits) {
      martianBuilder.insert(0, NUMEROLOGY.charAt(0));
    }

    return martianBuilder.toString();
  }

  /**
   * Decodes a Martian base-34 encoded string back into the original base-10 {@code long} value,
   * reversing the mix constant that was applied during encoding.
   *
   * @param base34 the base-34 encoded string to decode
   * @param group  the encoding group whose mix constant was used during encoding
   * @return the decoded non-negative decimal value
   * @throws IllegalArgumentException if the string contains characters not in the base-34 alphabet
   */
  public static long base34To10 (String base34, Group group) {

    long base10 = 0;
    long multiplier = 1;
    int martianNumber;

    for (int index = base34.length() - 1; index >= 0; index--) {
      if ((martianNumber = NUMEROLOGY.indexOf(base34.charAt(index))) < 0) {
        throw new IllegalArgumentException("Not a Martian base 34 number(" + base34 + ")");
      }

      base10 += martianNumber * multiplier;
      multiplier *= 34;
    }

    base10 -= group.getMixConstant();
    if (base10 < 0) {
      base10 += getMaxMartian(base34.length());
    }

    return (base10 >= 0) ? base10 : (Long.MAX_VALUE + base10 + 1) * -1;
  }
}
