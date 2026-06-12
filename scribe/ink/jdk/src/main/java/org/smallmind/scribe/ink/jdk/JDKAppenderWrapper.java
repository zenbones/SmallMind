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
 * JUL {@link Handler} that wraps a scribe {@link Appender}, bridging JUL's publish/flush/close lifecycle
 * and filter, formatter, and error-manager accessors to their scribe equivalents.
 */
public class JDKAppenderWrapper extends Handler {

  private final Appender appender;

  /**
   * Constructs a JUL {@link Handler} that delegates all publishing activity to the given scribe appender.
   *
   * @param appender the scribe appender to wrap
   */
  public JDKAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  /**
   * Returns the scribe appender that this handler delegates to.
   *
   * @return the wrapped scribe appender
   */
  protected Appender getInnerAppender () {

    return appender;
  }

  /**
   * Not supported; character encoding is managed by the underlying scribe appender.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  public String getEncoding () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Not supported; character encoding is managed by the underlying scribe appender.
   *
   * @param encoding ignored
   * @throws UnsupportedOperationException always
   */
  public void setEncoding (String encoding)
    throws SecurityException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Not supported; level filtering is managed by the scribe layer, not the JUL handler.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  public Level getLevel () {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Not supported; level filtering is managed by the scribe layer, not the JUL handler.
   *
   * @param newLevel ignored
   * @throws UnsupportedOperationException always
   */
  public void setLevel (Level newLevel)
    throws SecurityException {

    throw new UnsupportedOperationException("Unsupported native JDK Logging method");
  }

  /**
   * Returns the native JUL {@link Formatter} from the wrapped appender if it is a {@link FormattedAppender}
   * and its formatter is a {@link JDKFormatterAdapter}.
   *
   * @return the native JUL formatter, or {@code null} if none is configured
   * @throws UnsupportedOperationException if the appender's formatter is not a {@link JDKFormatterAdapter}
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
   * Wraps the given JUL formatter in a {@link JDKFormatterAdapter} and installs it on the underlying
   * {@link FormattedAppender}.
   *
   * @param formatter the JUL formatter to install
   * @throws UnsupportedOperationException if the wrapped appender does not implement {@link FormattedAppender}
   */
  public void setFormatter (Formatter formatter) {

    if (!(appender instanceof FormattedAppender)) {
      throw new UnsupportedOperationException("Appender (" + appender.getClass().getName() + "is not a FormattedAppender");
    }

    ((FormattedAppender)appender).setFormatter(new JDKFormatterAdapter(formatter));
  }

  /**
   * Returns the native JUL {@link Filter} from the first filter slot of the wrapped appender if it is a
   * {@link JDKFilterAdapter}.
   *
   * @return the native JUL filter, or {@code null} if no filters are configured
   * @throws UnsupportedOperationException if the first filter is not a {@link JDKFilterAdapter}
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
   * Replaces the appender's current filters with a single {@link JDKFilterAdapter} wrapping the given JUL filter.
   *
   * @param filter the JUL filter to install on the underlying appender
   */
  public void setFilter (Filter filter) {

    appender.clearFilters();
    appender.addFilter(new JDKFilterAdapter(filter));
  }

  /**
   * Checks every filter on the wrapped appender against the given JUL record by unwrapping each to its native
   * JUL filter; returns {@code false} if any filter rejects the record.
   *
   * @param record the JUL record to evaluate
   * @return {@code true} if all filters allow the record, {@code false} if any veto it
   * @throws UnsupportedOperationException if a filter on the appender is not a {@link JDKFilterAdapter}
   */
  public boolean isLoggable (LogRecord record) {

    for (org.smallmind.scribe.pen.Filter filter : appender.getFilters()) {
      if (!(filter instanceof JDKFilterAdapter)) {
        throw new UnsupportedOperationException("Encountered a non-JDK Logging native Filter(" + filter.getClass().getCanonicalName() + ")");
      } else if (!((JDKFilterAdapter)filter).getNativeFilter().isLoggable(record)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the native JUL {@link ErrorManager} from the wrapped appender's error handler if it is a
   * {@link JDKErrorHandlerAdapter}.
   *
   * @return the native error manager, or {@code null} if none is configured
   * @throws UnsupportedOperationException if the appender's error handler is not a {@link JDKErrorHandlerAdapter}
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
   * Wraps the given JUL error manager in a {@link JDKErrorHandlerAdapter} and installs it on the
   * underlying appender.
   *
   * @param errorManager the JUL error manager to install
   */
  public void setErrorManager (ErrorManager errorManager) {

    appender.setErrorHandler(new JDKErrorHandlerAdapter(errorManager));
  }

  /**
   * Extracts the scribe {@link org.smallmind.scribe.pen.Record} from the JUL record via the
   * {@link RecordWrapper} interface and publishes it to the underlying appender if the appender is active.
   *
   * @param record the JUL record to publish; must implement {@link RecordWrapper}
   */
  public void publish (LogRecord record) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)record).getRecord());
    }
  }

  /**
   * No-op flush; the underlying scribe appender manages its own buffering.
   */
  public void flush () {

  }

  /**
   * Closes the underlying scribe appender, wrapping any {@link LoggerException} or
   * {@link InterruptedException} in a {@link SecurityException} to satisfy the JUL contract.
   *
   * @throws SecurityException if the underlying appender throws during close or the thread is interrupted
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
   * Returns the hash code of the underlying scribe appender.
   *
   * @return hash code delegated to the wrapped appender
   */
  public int hashCode () {

    return appender.hashCode();
  }

  /**
   * Compares this wrapper for equality by comparing the underlying scribe appender; unwraps the other
   * object if it is also a {@link JDKAppenderWrapper}.
   *
   * @param obj the object to compare against
   * @return {@code true} if both wrappers delegate to the same appender, or the appender equals {@code obj} directly
   */
  public boolean equals (Object obj) {

    if (obj instanceof JDKAppenderWrapper) {
      return appender.equals(((JDKAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}
