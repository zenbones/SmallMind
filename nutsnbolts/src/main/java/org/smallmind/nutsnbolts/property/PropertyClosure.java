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
 * Holds the prefix and suffix delimiters used to detect property placeholders in an expression,
 * along with an optional {@link EncryptedVariation} for handling encrypted values.
 */
public class PropertyClosure {

  private final EncryptedVariation encryptedVariation;
  private final String prefix;
  private final String suffix;

  /**
   * Constructs a closure using the default delimiters {@code ${}} and {@code }} with no encrypted variation.
   *
   * @throws PropertyExpanderException if the default delimiters are invalid
   */
  public PropertyClosure ()
    throws PropertyExpanderException {

    this(null, "${", "}");
  }

  /**
   * Constructs a closure with caller-supplied delimiters and no encrypted variation.
   *
   * @param prefix the non-blank string that marks the start of a property placeholder
   * @param suffix the non-blank string that marks the end of a property placeholder
   * @throws PropertyExpanderException if either delimiter is blank or they share a character
   */
  public PropertyClosure (String prefix, String suffix)
    throws PropertyExpanderException {

    this(null, prefix, suffix);
  }

  /**
   * Constructs a closure with default delimiters and an encrypted variation using the supplied decryptor.
   *
   * @param decryptor the decryptor to use for encrypted property values
   * @throws PropertyExpanderException if the default delimiters are invalid
   */
  public PropertyClosure (Decryptor decryptor)
    throws PropertyExpanderException {

    this(new EncryptedVariation(decryptor), "${", "}");
  }

  /**
   * Constructs a closure with caller-supplied property delimiters, a custom encrypted prefix, and a decryptor.
   *
   * @param decryptor       the decryptor to use for encrypted property values
   * @param encryptedPrefix the prefix that marks encrypted placeholders within expressions
   * @param prefix          the non-blank string marking the start of any property placeholder
   * @param suffix          the non-blank string marking the end of any property placeholder
   * @throws PropertyExpanderException if any delimiter is invalid or shares characters with the suffix
   */
  public PropertyClosure (Decryptor decryptor, String encryptedPrefix, String prefix, String suffix)
    throws PropertyExpanderException {

    this(new EncryptedVariation(decryptor, encryptedPrefix), "${", "}");
  }

  /**
   * Primary internal constructor; validates delimiter constraints and stores all configuration.
   *
   * @param encryptedVariation optional encrypted-placeholder configuration; may be {@code null}
   * @param prefix             the non-blank placeholder start delimiter
   * @param suffix             the non-blank placeholder end delimiter
   * @throws PropertyExpanderException if any delimiter is null, blank, or shares characters with the suffix
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
   * Returns the suffix delimiter that marks the end of a property placeholder.
   *
   * @return the configured suffix string
   */
  public String getSuffix () {

    return suffix;
  }

  /**
   * Scans the expression for the next property placeholder starting at the given position and returns
   * a {@link PropertyPrologue} describing the prefix type, position, and applicable decryptor.
   *
   * @param expansionBuilder the current expression being expanded
   * @param parsePos         the index within the builder at which to begin the search
   * @return a descriptor for the next placeholder found, with a negative position if none exists
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
