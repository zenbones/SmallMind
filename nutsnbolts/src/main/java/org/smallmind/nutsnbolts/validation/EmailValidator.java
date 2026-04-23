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
package org.smallmind.nutsnbolts.validation;

import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link jakarta.validation.ConstraintValidator} for the {@link Email} constraint that validates strings against a basic email-address pattern.
 * Supports validating a single address or a delimited list of addresses when the {@link Email#separator()} is configured.
 */
public class EmailValidator implements ConstraintValidator<Email, String> {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%'+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

  private Email constraintAnnotation;

  /**
   * Returns {@code true} if the supplied string matches the email-address pattern.
   *
   * @param possibility the candidate string to test
   * @return {@code true} when the string is a valid email address
   */
  public static boolean isAnEmail (String possibility) {

    return EMAIL_PATTERN.matcher(possibility).matches();
  }

  /**
   * Stores the constraint annotation so that the configured separator is available during validation.
   *
   * @param constraintAnnotation the annotation instance driving this validation
   */
  @Override
  public void initialize (Email constraintAnnotation) {

    this.constraintAnnotation = constraintAnnotation;
  }

  /**
   * Validates that the value is a properly formatted email address, or a separator-delimited list of valid addresses.
   * A {@code null} value is considered valid.
   *
   * @param value   the candidate string to validate
   * @param context the constraint validator context (unused)
   * @return {@code true} if the value is {@code null} or satisfies the email format requirements
   */
  @Override
  public boolean isValid (String value, ConstraintValidatorContext context) {

    if (value == null) {
      return true;
    }

    if (constraintAnnotation.separator() == '\0') {

      return isAnEmail(value);
    } else {

      int lastIndex = 0;

      for (int index = 0; index < value.length(); index++) {
        if (value.charAt(index) == constraintAnnotation.separator()) {
          if (!isAnEmail(value.substring(lastIndex, index).strip())) {

            return false;
          }

          lastIndex = index + 1;
        }
      }

      return isAnEmail(value.substring(lastIndex).strip());
    }
  }
}
