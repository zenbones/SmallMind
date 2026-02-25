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
package org.smallmind.nutsnbolts.reflection;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Validates whether a {@link LocalDateTime} value represents the Unix epoch instant and should be
 * treated as equivalent to {@code null}.
 */
public class StartOfEpochNullifierValidator implements OverlayNullifierValidator<StartOfEpochNullifier, LocalDateTime> {

  private StartOfEpochNullifier constraintAnnotation;

  /**
   * Initializes this validator with the constraint configuration declared on the target field.
   *
   * @param constraintAnnotation the annotation instance containing validator settings
   */
  @Override
  public void initialize (StartOfEpochNullifier constraintAnnotation) {

    this.constraintAnnotation = constraintAnnotation;
  }

  /**
   * Determines whether the supplied date-time resolves to epoch millisecond {@code 0} in the
   * configured zone.
   *
   * @param date the date-time value to evaluate
   * @return {@code true} if the value maps to the start of the Unix epoch; otherwise {@code false}
   */
  @Override
  public boolean equivalentToNull (LocalDateTime date) {

    return date.atZone(ZoneId.of(constraintAnnotation.zoneId())).toInstant().toEpochMilli() == 0;
  }
}
