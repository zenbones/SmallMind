/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.scribe.ink.log4j;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.AbstractFormattedAppender;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class Log4JAppenderWrapper implements org.apache.log4j.Appender {

  private Appender appender;

  public Log4JAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  protected Appender getInnerAppender () {

    return appender;
  }

  public String getName () {

    return appender.getName();
  }

  public void setName (String name) {

    appender.setName(name);
  }

  public void addFilter (Filter filter) {

    appender.addFilter(new Log4JFilterAdapter(filter));
  }

  public Filter getFilter () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  public void clearFilters () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  public void doAppend (LoggingEvent loggingEvent) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)loggingEvent).getRecord());
    }
  }

  public void close () {

    // Log4J will close all Appenders when removing them from a Logger, even though under Log4J
    // Appenders are shared objects. So we can't let Log4J handle the close method> Bad Log4J.
  }

  public ErrorHandler getErrorHandler () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  public void setErrorHandler (ErrorHandler errorHandler) {

    appender.setErrorHandler(new Log4JErrorHandlerAdapter(errorHandler));
  }

  public Layout getLayout () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  public void setLayout (Layout layout) {

    appender.setFormatter(new Log4JFormatterAdapter(layout));
  }

  public boolean requiresLayout () {

    return appender instanceof AbstractFormattedAppender;
  }

  public int hashCode () {

    return appender.hashCode();
  }

  public boolean equals (Object obj) {

    if (obj instanceof Log4JAppenderWrapper) {
      return appender.equals(((Log4JAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }

  protected void finalize () {

    close();
  }
}