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
 * Pairs a {@link Decryptor} with a prefix string that identifies encrypted property placeholders
 * within an expression, allowing {@link PropertyClosure} to distinguish them from plain ones.
 */
public class EncryptedVariation {

  private final Decryptor decryptor;
  private final String prefix;

  /**
   * Constructs an encrypted variation with the default encrypted prefix {@code "!{"}.
   *
   * @param decryptor the decryptor that will be applied to resolved values bearing this prefix
   * @throws PropertyExpanderException if the default prefix would be invalid
   * @throws NullPointerException      if {@code decryptor} is {@code null}
   */
  public EncryptedVariation (Decryptor decryptor)
    throws PropertyExpanderException {

    this(decryptor, "!{");
  }

  /**
   * Constructs an encrypted variation with a caller-supplied prefix and decryptor.
   *
   * @param decryptor the decryptor that will be applied to resolved values bearing this prefix
   * @param prefix    the non-blank prefix string that marks encrypted placeholders
   * @throws PropertyExpanderException if {@code prefix} is blank
   * @throws NullPointerException      if {@code decryptor} or {@code prefix} is {@code null}
   */
  public EncryptedVariation (Decryptor decryptor, String prefix)
    throws PropertyExpanderException {

    if ((decryptor == null) || (prefix == null)) {
      throw new NullPointerException();
    } else if (prefix.isBlank()) {
      throw new PropertyExpanderException("The prefix may not be blank");
    }

    this.decryptor = decryptor;
    this.prefix = prefix;
  }

  /**
   * Returns the decryptor associated with this variation.
   *
   * @return the {@link Decryptor} to use when an encrypted placeholder is resolved
   */
  public Decryptor getDecryptor () {

    return decryptor;
  }

  /**
   * Returns the prefix string that marks encrypted property placeholders in an expression.
   *
   * @return the prefix used to identify encrypted values
   */
  public String getPrefix () {

    return prefix;
  }
}
