/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.scribe.pen;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class TimestampRolloverRule implements RolloverRule {

  private TimestampQuantifier timestampQuantifier;

  public TimestampRolloverRule () {

    this(TimestampQuantifier.TOP_OF_DAY);
  }

  public TimestampRolloverRule (TimestampQuantifier timestampQuantifier) {

    super();

    this.timestampQuantifier = timestampQuantifier;
  }

  public TimestampQuantifier getTimestampQuantifier () {

    return timestampQuantifier;
  }

  public void setTimestampQuantifier (TimestampQuantifier timestampQuantifier) {

    this.timestampQuantifier = timestampQuantifier;
  }

  public boolean willRollover (long fileSize, long lastModifiedTimeified, long bytesToBeWritten) {

    ZonedDateTime lastModifiedTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastModifiedTimeified), ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    switch (timestampQuantifier) {
      case TOP_OF_MINUTE:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue()) || (now.getDayOfMonth() != lastModifiedTime.getDayOfMonth()) || (now.getHour() != lastModifiedTime.getHour()) || (now.getMinute() != lastModifiedTime.getMinute());
      case TOP_OF_HOUR:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue()) || (now.getDayOfMonth() != lastModifiedTime.getDayOfMonth()) || (now.getHour() != lastModifiedTime.getHour());
      case HALF_DAY:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue()) || (now.getDayOfMonth() != lastModifiedTime.getDayOfMonth()) || (now.get(ChronoField.AMPM_OF_DAY) != lastModifiedTime.get(ChronoField.AMPM_OF_DAY));
      case TOP_OF_DAY:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue()) || (now.getDayOfMonth() != lastModifiedTime.getDayOfMonth());
      case TOP_OF_WEEK:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue()) || (now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != lastModifiedTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
      case TOP_OF_MONTH:
        return (now.getYear() != lastModifiedTime.getYear()) || (now.getMonthValue() != lastModifiedTime.getMonthValue());
      default:
        throw new UnknownSwitchCaseException(timestampQuantifier.name());
    }
  }
}