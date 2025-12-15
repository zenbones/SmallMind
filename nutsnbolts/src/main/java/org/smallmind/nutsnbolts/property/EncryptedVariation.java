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
 * Variation handler that marks property values requiring decryption. Uses a prefix to detect encrypted
 * values and supplies the {@link Decryptor} to perform decryption.
 */
public class EncryptedVariation {

  private final Decryptor decryptor;
  private final String prefix;

  /**
   * Creates an encrypted variation using the default prefix {@code "!{"}.
   *
   * @param decryptor the decryptor used to decrypt property values
   * @throws PropertyExpanderException if the prefix is invalid
   * @throws NullPointerException      if decryptor is {@code null}
   */
  public EncryptedVariation (Decryptor decryptor)
    throws PropertyExpanderException {

    this(decryptor, "!{");
  }

  /**
   * Creates an encrypted variation with a custom prefix.
   *
   * @param decryptor the decryptor used to decrypt property values
   * @param prefix    the prefix that indicates encrypted text
   * @throws PropertyExpanderException if the prefix is blank
   * @throws NullPointerException      if decryptor or prefix is {@code null}
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
   * @return the decryptor used for encrypted values
   */
  public Decryptor getDecryptor () {

    return decryptor;
  }

  /**
   * @return the prefix that denotes encrypted property values
   */
  public String getPrefix () {

    return prefix;
  }
}
