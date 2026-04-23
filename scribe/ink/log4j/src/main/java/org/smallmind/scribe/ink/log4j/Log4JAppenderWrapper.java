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
package org.smallmind.scribe.ink.log4j;

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * Log4j2 {@link org.apache.logging.log4j.core.Appender} that wraps a scribe {@link Appender},
 * reporting a permanently started lifecycle state and delegating {@link #append} calls to the
 * underlying scribe appender when it is active.
 */
public class Log4JAppenderWrapper implements org.apache.logging.log4j.core.Appender {

  private final Appender appender;

  /**
   * Constructs a Log4j2 appender that delegates publishing to the given scribe appender.
   *
   * @param appender the scribe appender to wrap
   */
  public Log4JAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  /**
   * Returns the scribe appender that this wrapper delegates to.
   *
   * @return the wrapped scribe appender
   */
  protected Appender getInnerAppender () {

    return appender;
  }

  /**
   * Returns the name of the underlying scribe appender.
   *
   * @return the appender name
   */
  @Override
  public String getName () {

    return appender.getName();
  }

  /**
   * Returns {@code false} so that exceptions thrown during appending propagate to the caller.
   *
   * @return {@code false} always
   */
  @Override
  public boolean ignoreExceptions () {

    return false;
  }

  /**
   * Returns {@link State#STARTED} because the lifecycle of the wrapped appender is managed externally.
   *
   * @return {@link State#STARTED} always
   */
  @Override
  public State getState () {

    return State.STARTED;
  }

  /**
   * No-op; initialization is handled by the underlying scribe appender.
   */
  @Override
  public void initialize () {

  }

  /**
   * No-op; the start lifecycle is managed by the underlying scribe appender.
   */
  @Override
  public void start () {

  }

  /**
   * No-op; the stop lifecycle is managed by the underlying scribe appender.
   */
  @Override
  public void stop () {

  }

  /**
   * Returns {@code true} because this wrapper always reports itself as started.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isStarted () {

    return true;
  }

  /**
   * Returns {@code false} because this wrapper never reports itself as stopped.
   *
   * @return {@code false} always
   */
  @Override
  public boolean isStopped () {

    return false;
  }

  /**
   * Extracts the scribe {@link org.smallmind.scribe.pen.Record} from the Log4j2 event via the
   * {@link RecordWrapper} interface and publishes it to the underlying scribe appender if active.
   *
   * @param logEvent the Log4j2 event to publish; must implement {@link RecordWrapper}
   */
  @Override
  public void append (LogEvent logEvent) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)logEvent).getRecord());
    }
  }

  /**
   * Not supported; error handling is managed through the scribe appender's own error handler.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  @Override
  public ErrorHandler getHandler () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  /**
   * Wraps the given Log4j2 error handler in a {@link Log4JErrorHandlerAdapter} and installs it on the
   * underlying scribe appender.
   *
   * @param errorHandler the Log4j2 error handler to install
   */
  @Override
  public void setHandler (ErrorHandler errorHandler) {

    appender.setErrorHandler(new Log4JErrorHandlerAdapter(errorHandler));
  }

  /**
   * Not supported; layout configuration is managed through the scribe appender's own formatter.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  @Override
  public Layout<?> getLayout () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  /**
   * Returns the hash code of the underlying scribe appender.
   *
   * @return hash code delegated to the wrapped appender
   */
  @Override
  public int hashCode () {

    return appender.hashCode();
  }

  /**
   * Compares this wrapper for equality by comparing the underlying scribe appender; unwraps the other
   * object if it is also a {@link Log4JAppenderWrapper}.
   *
   * @param obj the object to compare against
   * @return {@code true} if both wrappers delegate to the same appender, or the appender equals {@code obj} directly
   */
  @Override
  public boolean equals (Object obj) {

    if (obj instanceof Log4JAppenderWrapper) {
      return appender.equals(((Log4JAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}
