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
 * Encapsulates the delimiters used for property expansion and optional handling of encrypted values.
 * Supports locating the next property prologue within an expression string.
 */
public class PropertyClosure {

  private final EncryptedVariation encryptedVariation;
  private final String prefix;
  private final String suffix;

  /**
   * Creates a closure using default delimiters {@code ${} } and no encrypted variation.
   *
   * @throws PropertyExpanderException if delimiters are invalid
   */
  public PropertyClosure ()
    throws PropertyExpanderException {

    this(null, "${", "}");
  }

  /**
   * Creates a closure with custom delimiters and no encrypted variation.
   *
   * @param prefix the prefix marking the start of a property
   * @param suffix the suffix marking the end of a property
   * @throws PropertyExpanderException if delimiters are invalid
   */
  public PropertyClosure (String prefix, String suffix)
    throws PropertyExpanderException {

    this(null, prefix, suffix);
  }

  /**
   * Creates a closure with default delimiters and an encrypted variation using the provided decryptor.
   *
   * @param decryptor decryptor used for encrypted values
   * @throws PropertyExpanderException if delimiters are invalid
   */
  public PropertyClosure (Decryptor decryptor)
    throws PropertyExpanderException {

    this(new EncryptedVariation(decryptor), "${", "}");
  }

  /**
   * Creates a closure with default property delimiters and a custom encrypted prefix.
   *
   * @param decryptor       decryptor used for encrypted values
   * @param encryptedPrefix prefix denoting encrypted property values
   * @param prefix          property prefix delimiter
   * @param suffix          property suffix delimiter
   * @throws PropertyExpanderException if delimiters are invalid
   */
  public PropertyClosure (Decryptor decryptor, String encryptedPrefix, String prefix, String suffix)
    throws PropertyExpanderException {

    this(new EncryptedVariation(decryptor, encryptedPrefix), "${", "}");
  }

  /**
   * Internal constructor for fully-specified closure and encrypted variation data.
   *
   * @param encryptedVariation optional encrypted variation configuration
   * @param prefix             property prefix delimiter
   * @param suffix             property suffix delimiter
   * @throws PropertyExpanderException if delimiters are invalid or overlap
   */
  private PropertyClosure (EncryptedVariation encryptedVariation, String prefix, String suffix)
    throws PropertyExpanderException {

    this.encryptedVariation = encryptedVariation;
    this.prefix = prefix;
    this.suffix = suffix;

    if ((prefix == null) || (suffix == null)) {
      throw new PropertyExpanderException();
    } else if (prefix.isBlank() || suffix.isBlank()) {
      throw new PropertyExpanderException("Neither the prefix nor suffix may be blank");
    }

    for (int pos = 0; pos < prefix.length(); pos++) {
      if (suffix.indexOf(prefix.charAt(pos)) >= 0) {
        throw new PropertyExpanderException("The prefix(%s) and suffix(%s) should have no characters in common", prefix, suffix);
      }
    }
    if (encryptedVariation != null) {
      for (int pos = 0; pos < encryptedVariation.getPrefix().length(); pos++) {
        if (suffix.indexOf(encryptedVariation.getPrefix().charAt(pos)) >= 0) {
          throw new PropertyExpanderException("The encrypted prefix(%s) and suffix(%s) should have no characters in common", encryptedVariation.getPrefix(), suffix);
        }
      }
    }
  }

  /**
   * @return the property suffix delimiter
   */
  public String getSuffix () {

    return suffix;
  }

  /**
   * Finds the next property prologue (either encrypted or plain) starting at {@code parsePos}.
   *
   * @param expansionBuilder the expression being parsed
   * @param parsePos         the index to begin searching
   * @return a {@link PropertyPrologue} describing the next property occurrence
   */
  public PropertyPrologue findPrologue (StringBuilder expansionBuilder, int parsePos) {

    int prefixPos = expansionBuilder.indexOf(prefix, parsePos);
    int encryptedPrefixPos = (encryptedVariation == null) ? -1 : expansionBuilder.indexOf(encryptedVariation.getPrefix(), parsePos);

    if (encryptedPrefixPos < 0) {

      return new PropertyPrologue(null, prefix, prefixPos);
    } else if ((prefixPos < 0) || (encryptedPrefixPos < prefixPos)) {

      return new PropertyPrologue(encryptedVariation.getDecryptor(), encryptedVariation.getPrefix(), encryptedPrefixPos);
    } else {

      return new PropertyPrologue(null, prefix, prefixPos);
    }
  }
}
