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
package org.smallmind.scribe.ink.jdk;

import java.io.UnsupportedEncodingException;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class JDKAppenderWrapper extends Handler {

  private final Appender appender;

  public JDKAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  protected Appender getInnerAppender () {

    return appender;
  }

  public String getEncoding () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  public void setEncoding (String encoding)
    throws SecurityException, UnsupportedEncodingException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  public Level getLevel () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  public void setLevel (Level newLevel)
    throws SecurityException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  public Formatter getFormatter () {

    org.smallmind.scribe.pen.Formatter formatter;

    if ((formatter = appender.getFormatter()) != null) {
      if (!(formatter instanceof JDKFormatterAdapter)) {
        throw new UnsupportedOperationException("Can not return a non-JDK Logging native Formatter(" + formatter.getClass().getCanonicalName() + ")");
      }

      return ((JDKFormatterAdapter)formatter).getNativeFormatter();
    }

    return null;
  }

  public void setFormatter (Formatter formatter) {

    appender.setFormatter(new JDKFormatterAdapter(formatter));
  }

  public Filter getFilter () {

    org.smallmind.scribe.pen.Filter[] filters;

    if ((filters = appender.getFilters()).length > 0) {
      if (!(filters[0] instanceof JDKFilterAdapter)) {
        throw new UnsupportedOperationException("Can not return a non-JDK Logging native Filter(" + filters[0].getClass().getCanonicalName() + ")");
      }

      return ((JDKFilterAdapter)filters[0]).getNativeFilter();
    }

    return null;
  }

  public void setFilter (Filter filter) {

    appender.clearFilters();
    appender.addFilter(new JDKFilterAdapter(filter));
  }

  public boolean isLoggable (LogRecord record) {

    for (org.smallmind.scribe.pen.Filter filter : appender.getFilters()) {
      if (!(filter instanceof JDKFilterAdapter)) {
        throw new UnsupportedOperationException("Encountered a non-JDK Logging native Filter(" + filter.getClass().getCanonicalName() + ")");
      } else if (!((JDKFilterAdapter)filter).getNativeFilter().isLoggable(record)) {
        return false;
      }
    }

    return false;
  }

  public ErrorManager getErrorManager () {

    org.smallmind.scribe.pen.ErrorHandler errorHandler;

    if ((errorHandler = appender.getErrorHandler()) != null) {
      if (!(errorHandler instanceof JDKErrorHandlerAdapter)) {
        throw new UnsupportedOperationException("Can not return a non-JDK Logging native ErrorManager(" + errorHandler.getClass().getCanonicalName() + ")");
      }

      return ((JDKErrorHandlerAdapter)errorHandler).getNativeErrorManager();
    }

    return null;
  }

  public void setErrorManager (ErrorManager errorManager) {

    appender.setErrorHandler(new JDKErrorHandlerAdapter(errorManager));
  }

  public void publish (LogRecord record) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)record).getRecord());
    }
  }

  public void flush () {

  }

  public void close ()
    throws SecurityException {

    try {
      appender.close();
    } catch (LoggerException | InterruptedException exception) {
      throw new SecurityException(exception);
    }
  }

  public int hashCode () {

    return appender.hashCode();
  }

  protected void finalize () {

    close();
  }

  public boolean equals (Object obj) {

    if (obj instanceof JDKAppenderWrapper) {
      return appender.equals(((JDKAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}