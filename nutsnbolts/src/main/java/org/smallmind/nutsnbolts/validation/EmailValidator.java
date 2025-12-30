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
 * Bean Validation validator that checks strings against a basic email pattern.
 * Supports validating a single address or multiple addresses separated by a configured delimiter.
 */
public class EmailValidator implements ConstraintValidator<Email, String> {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9_\\-.']+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(]?)");

  private Email constraintAnnotation;

  /**
   * Stores the constraint annotation for later use (separator configuration).
   *
   * @param constraintAnnotation annotation instance being used for validation
   */
  @Override
  public void initialize (Email constraintAnnotation) {

    this.constraintAnnotation = constraintAnnotation;
  }

  /**
   * Validates that the value is a properly formatted email address (or list of addresses when a separator is defined).
   *
   * @param value   candidate string; {@code null} is considered valid
   * @param context validation context (unused)
   * @return {@code true} when the value meets the email format requirements
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

  /**
   * Tests a single string against the email pattern.
   *
   * @param possibility candidate email
   * @return {@code true} when the pattern matches
   */
  private boolean isAnEmail (String possibility) {

    return EMAIL_PATTERN.matcher(possibility).matches();
  }
}
