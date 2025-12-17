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
 * Wraps a scribe {@link Appender} as a Log4j2 {@link org.apache.logging.log4j.core.Appender}.
 */
public class Log4JAppenderWrapper implements org.apache.logging.log4j.core.Appender {

  private final Appender appender;

  /**
   * Creates a wrapper that delegates Log4j2 appender calls to the provided scribe appender.
   *
   * @param appender appender to wrap
   */
  public Log4JAppenderWrapper (Appender appender) {

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
   * Returns the name of the wrapped appender.
   *
   * @return appender name
   */
  @Override
  public String getName () {

    return appender.getName();
  }

  /**
   * Indicates whether the adapter ignores exceptions (it does not).
   *
   * @return {@code false} to let exceptions propagate
   */
  @Override
  public boolean ignoreExceptions () {

    return false;
  }

  /**
   * Returns a started state since the wrapped appender is managed externally.
   *
   * @return {@link State#STARTED}
   */
  @Override
  public State getState () {

    return State.STARTED;
  }

  /**
   * No-op initialization.
   */
  @Override
  public void initialize () {

  }

  /**
   * No-op start; lifecycle is managed by the wrapped appender.
   */
  @Override
  public void start () {

  }

  /**
   * No-op stop; lifecycle is managed by the wrapped appender.
   */
  @Override
  public void stop () {

  }

  /**
   * Indicates that this adapter is considered started.
   *
   * @return {@code true}
   */
  @Override
  public boolean isStarted () {

    return true;
  }

  /**
   * Indicates that this adapter is never considered stopped.
   *
   * @return {@code false}
   */
  @Override
  public boolean isStopped () {

    return false;
  }

  /**
   * Publishes a log event to the wrapped appender when active.
   *
   * @param logEvent native Log4j2 event carrying the scribe record wrapper
   */
  @Override
  public void append (LogEvent logEvent) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)logEvent).getRecord());
    }
  }

  /**
   * Unsupported in this adapter; handlers are set on the wrapped appender.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always thrown
   */
  @Override
  public ErrorHandler getHandler () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  /**
   * Installs a Log4j2 error handler by wrapping it in a scribe adapter.
   *
   * @param errorHandler Log4j2 error handler to use
   */
  @Override
  public void setHandler (ErrorHandler errorHandler) {

    appender.setErrorHandler(new Log4JErrorHandlerAdapter(errorHandler));
  }

  /**
   * Unsupported in this adapter; layout is configured on the wrapped appender.
   *
   * @return never returns normally
   * @throws UnsupportedOperationException always thrown
   */
  @Override
  public Layout<?> getLayout () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  /**
   * Computes the hash code based on the wrapped appender.
   *
   * @return hash code
   */
  @Override
  public int hashCode () {

    return appender.hashCode();
  }

  /**
   * Compares this wrapper to another object based on the underlying appender.
   *
   * @param obj object to compare against
   * @return {@code true} if the wrapped appenders are equal
   */
  @Override
  public boolean equals (Object obj) {

    if (obj instanceof Log4JAppenderWrapper) {
      return appender.equals(((Log4JAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}
