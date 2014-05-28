/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import org.smallmind.scribe.pen.adapter.LoggingBlueprintsFactory;

public class DefaultErrorHandler implements ErrorHandler {

  private Appender appender;

  public DefaultErrorHandler () {

    appender = new ConsoleAppender(new XMLFormatter());
  }

  public DefaultErrorHandler (Appender appender) {

    setBackupAppender(appender);
  }

  public void setBackupAppender (Appender appender) {

    this.appender = appender;
  }

  public void process (Record record, Exception exception, String errorMessage, Object... args) {

    appender.publish(LoggingBlueprintsFactory.getLoggingBlueprints().errorRecord(record, exception, errorMessage, args));
    appender.publish(record);
  }
}