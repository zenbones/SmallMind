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
package org.smallmind.nutsnbolts.property;

import org.smallmind.nutsnbolts.security.kms.Decryptor;

/**
 * Captures the location, prefix text, and optional decryptor for a single property placeholder
 * occurrence discovered during expression scanning.
 */
public class PropertyPrologue {

  private final Decryptor decryptor;
  private final String prefix;
  private final int pos;

  /**
   * Constructs a prologue record for the next placeholder found in an expression.
   *
   * @param decryptor the decryptor to apply after resolving the placeholder value, or {@code null}
   *                  when the placeholder is not encrypted
   * @param prefix    the prefix text that was matched at this position
   * @param pos       the index of the first character of the prefix within the expression, or
   *                  a negative value when no placeholder was found
   */
  public PropertyPrologue (Decryptor decryptor, String prefix, int pos) {

    this.decryptor = decryptor;
    this.prefix = prefix;
    this.pos = pos;
  }

  /**
   * Returns the decryptor for this placeholder, or {@code null} when the placeholder is not encrypted.
   *
   * @return the applicable {@link Decryptor}, or {@code null}
   */
  public Decryptor getDecryptor () {

    return decryptor;
  }

  /**
   * Returns the prefix string that introduced this placeholder.
   *
   * @return the matched prefix text
   */
  public String getPrefix () {

    return prefix;
  }

  /**
   * Returns the index of the prefix within the expression, or a negative value when no placeholder was found.
   *
   * @return the index of the matched prefix in the expression string
   */
  public int getPos () {

    return pos;
  }
}
