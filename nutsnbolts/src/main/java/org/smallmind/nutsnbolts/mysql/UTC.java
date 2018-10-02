/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.nutsnbolts.mysql;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UTC {

  private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

  public static ZonedDateTime isoParse (String date) {

    return ZonedDateTime.from(ISO_DATE_TIME_FORMATTER.parse(date));
  }

  public static String isoFormat (Date date) {

    return ISO_DATE_TIME_FORMATTER.format(date.toInstant());
  }

  public static Date now () {

    Instant instant;

    return Date.from((instant = Instant.now()).minusSeconds(ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds()));
  }

  public static Date then (Date date) {

    Instant instant;

    return Date.from((instant = date.toInstant()).minusSeconds(ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds()));
  }

  public static Date local (Date date) {

    Instant instant;

    return Date.from((instant = date.toInstant()).plusSeconds(ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds()));
  }

  public static Date local (Date date, int offsetSeconds) {

    return Date.from(date.toInstant().plusSeconds(offsetSeconds));
  }
}
