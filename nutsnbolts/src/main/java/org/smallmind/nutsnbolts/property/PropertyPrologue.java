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
 * Represents a discovered property prologue within an expression, capturing its position,
 * prefix, and optional decryptor to apply to resolved values.
 */
public class PropertyPrologue {

  private final Decryptor decryptor;
  private final String prefix;
  private final int pos;

  /**
   * Creates a prologue descriptor.
   *
   * @param decryptor decryptor to apply if the property is encrypted; {@code null} for plain properties
   * @param prefix    the matched prefix text
   * @param pos       the index of the prefix in the expression
   */
  public PropertyPrologue (Decryptor decryptor, String prefix, int pos) {

    this.decryptor = decryptor;
    this.prefix = prefix;
    this.pos = pos;
  }

  /**
   * @return decryptor to apply, or {@code null} when not encrypted
   */
  public Decryptor getDecryptor () {

    return decryptor;
  }

  /**
   * @return the matched prefix text
   */
  public String getPrefix () {

    return prefix;
  }

  /**
   * @return the position of the prefix within the expression
   */
  public int getPos () {

    return pos;
  }
}
