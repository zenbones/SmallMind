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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.FormattedAppender;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * Wraps a scribe {@link Appender} as a JUL {@link Handler}, translating formatting, filtering,
 * and error handling interactions between the two APIs.
 */
public class JDKAppenderWrapper extends Handler {

  private final Appender appender;

  /**
   * Creates a handler that delegates publishing to the provided scribe appender.
   *
   * @param appender appender to wrap
   */
  public JDKAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  /**
   * Returns the wrapped scribe appender.
   *
   * @return the underlying appender
   */
  protected Appender getInnerAppender () {

    return appender;
  }

  /**
   * Unsupported in this adapter; native encoding is not exposed.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always thrown
   */
  public String getEncoding () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Unsupported in this adapter; encoding cannot be set via JUL.
   *
   * @param encoding ignored
   * @throws UnsupportedOperationException always thrown
   */
  public void setEncoding (String encoding)
    throws SecurityException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Unsupported in this adapter; level is managed by the scribe appender.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always thrown
   */
  public Level getLevel () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Unsupported in this adapter; level cannot be set via JUL.
   *
   * @param newLevel ignored
   * @throws UnsupportedOperationException always thrown
   */
  public void setLevel (Level newLevel)
    throws SecurityException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Returns the native JUL formatter if the wrapped appender exposes one.
   *
   * @return the JUL formatter or {@code null} if none configured
   * @throws UnsupportedOperationException if the configured formatter is not a JUL adapter
   */
  public Formatter getFormatter () {

    if (appender instanceof FormattedAppender) {

      org.smallmind.scribe.pen.Formatter formatter;

      if ((formatter = ((FormattedAppender)appender).getFormatter()) != null) {
        if (!(formatter instanceof JDKFormatterAdapter)) {
          throw new UnsupportedOperationException("Can not return a non-JDK Logging native Formatter(" + formatter.getClass().getCanonicalName() + ")");
        }

        return ((JDKFormatterAdapter)formatter).getNativeFormatter();
      }
    }

    return null;
  }

  /**
   * Installs a JUL formatter on the wrapped {@link FormattedAppender}.
   *
   * @param formatter JUL formatter to wrap
   * @throws UnsupportedOperationException if the wrapped appender does not support formatting
   */
  public void setFormatter (Formatter formatter) {

    if (!(appender instanceof FormattedAppender)) {
      throw new UnsupportedOperationException("Appender (" + appender.getClass().getName() + "is not a FormattedAppender");
    }

    ((FormattedAppender)appender).setFormatter(new JDKFormatterAdapter(formatter));
  }

  /**
   * Returns the native JUL filter from the wrapped appender if present.
   *
   * @return the JUL filter or {@code null} if none configured
   * @throws UnsupportedOperationException if the configured filter is not a JUL adapter
   */
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

  /**
   * Sets a JUL filter by wrapping it for the scribe appender.
   *
   * @param filter JUL filter to install
   */
  public void setFilter (Filter filter) {

    appender.clearFilters();
    appender.addFilter(new JDKFilterAdapter(filter));
  }

  /**
   * Evaluates whether the wrapped appender will log the supplied record.
   *
   * @param record JUL record carrying the scribe wrapper
   * @return {@code true} if all filters allow the record
   * @throws UnsupportedOperationException if a non-JUL filter is encountered
   */
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

  /**
   * Returns the native JUL error manager if configured on the appender.
   *
   * @return the JUL error manager or {@code null}
   * @throws UnsupportedOperationException if the configured error handler is not a JUL adapter
   */
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

  /**
   * Sets the error manager by wrapping it in a scribe error handler adapter.
   *
   * @param errorManager JUL error manager to use
   */
  public void setErrorManager (ErrorManager errorManager) {

    appender.setErrorHandler(new JDKErrorHandlerAdapter(errorManager));
  }

  /**
   * Publishes a record to the wrapped appender when it is active.
   *
   * @param record JUL record containing the scribe record wrapper
   */
  public void publish (LogRecord record) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)record).getRecord());
    }
  }

  /**
   * Flush is a no-op for the wrapped appender.
   */
  public void flush () {

  }

  /**
   * Closes the wrapped appender, translating any checked exceptions to {@link SecurityException}
   * as required by the JUL {@link Handler} contract.
   *
   * @throws SecurityException if closing the delegate fails or is interrupted
   */
  public void close ()
    throws SecurityException {

    try {
      appender.close();
    } catch (LoggerException | InterruptedException exception) {
      throw new SecurityException(exception);
    }
  }

  /**
   * Computes the hash code of this wrapper based on the wrapped appender.
   *
   * @return hash code
   */
  public int hashCode () {

    return appender.hashCode();
  }

  /**
   * Compares this wrapper to another object based on the underlying appender.
   *
   * @param obj object to compare against
   * @return {@code true} if the wrapped appenders are equal
   */
  public boolean equals (Object obj) {

    if (obj instanceof JDKAppenderWrapper) {
      return appender.equals(((JDKAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}
