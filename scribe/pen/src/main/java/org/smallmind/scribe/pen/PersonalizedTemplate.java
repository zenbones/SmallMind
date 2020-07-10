/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.concurrent.atomic.AtomicReference;

public class PersonalizedTemplate extends Template {

  private final AtomicReference<String> loggerNameRef = new AtomicReference<String>();

  public PersonalizedTemplate () {

    super();
  }

  public PersonalizedTemplate (String loggerName) {

    super();

    loggerNameRef.set(loggerName);
  }

  public PersonalizedTemplate (Level level, boolean autoFillLoggerContext, String loggerName)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    loggerNameRef.set(loggerName);
  }

  public PersonalizedTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String loggerName)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    loggerNameRef.set(loggerName);
  }

  public void setLoggerName (String loggerName) {

    if (!loggerNameRef.compareAndSet(null, loggerName)) {
      throw new LoggerRuntimeException("PersonalizedTemplate has been previously initialized with a logger name");
    }
  }

  public int matchLogger (String loggerName) {

    if (loggerNameRef.get() == null) {
      throw new LoggerRuntimeException("PersonalizedTemplate was never initialized with a logger name");
    }

    return loggerNameRef.get().equals(loggerName) ? Integer.MAX_VALUE : NO_MATCH;
  }
}

