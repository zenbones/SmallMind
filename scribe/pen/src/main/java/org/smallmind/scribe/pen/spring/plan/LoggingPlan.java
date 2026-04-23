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
package org.smallmind.scribe.pen.spring.plan;

import java.io.IOException;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.AsynchronousAppender;
import org.smallmind.scribe.pen.ClassNameTemplate;
import org.smallmind.scribe.pen.DefaultTemplate;
import org.smallmind.scribe.pen.Level;
import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract Spring {@link InitializingBean} base class for logging plans that wraps a concrete appender in an
 * {@link AsynchronousAppender}, registers a {@link DefaultTemplate} at the configured default level, and
 * optionally registers a {@link ClassNameTemplate} for each supplied {@link Log} entry.
 */
public abstract class LoggingPlan implements InitializingBean {

  private Log[] logs;
  private Level defaultLogLevel = Level.INFO;
  private int logRecordBufferSize = 400;
  private int concurrencyLimit = 1;

  /**
   * Constructs and returns the concrete {@link Appender} that this plan will wrap asynchronously; subclasses
   * provide the actual appender implementation (console, file, Fluent Bit, etc.).
   *
   * @return the configured appender for this plan
   * @throws IOException if the appender cannot be created or its underlying resource cannot be opened
   */
  public abstract Appender getAppender ()
    throws IOException;

  /**
   * Sets the level threshold applied to the {@link DefaultTemplate} registered during initialization;
   * defaults to {@link Level#INFO}.
   *
   * @param defaultLogLevel the minimum level at which the default template will emit records
   */
  public void setDefaultLogLevel (Level defaultLogLevel) {

    this.defaultLogLevel = defaultLogLevel;
  }

  /**
   * Sets the number of log records that the {@link AsynchronousAppender}'s internal queue can hold before
   * producers block; defaults to 400.
   *
   * @param logRecordBufferSize the capacity of the asynchronous record buffer
   */
  public void setLogRecordBufferSize (int logRecordBufferSize) {

    this.logRecordBufferSize = logRecordBufferSize;
  }

  /**
   * Sets the number of worker threads that the {@link AsynchronousAppender} uses to drain the record queue;
   * defaults to 1.
   *
   * @param concurrencyLimit the number of concurrent dispatch threads
   */
  public void setConcurrencyLimit (int concurrencyLimit) {

    this.concurrencyLimit = concurrencyLimit;
  }

  /**
   * Sets the array of {@link Log} entries whose class-name patterns and levels will each be registered as
   * a {@link ClassNameTemplate} during initialization; may be {@code null} if no per-logger overrides are needed.
   *
   * @param logs the per-logger level and pattern entries to register
   */
  public void setLogs (Log[] logs) {

    this.logs = logs;
  }

  /**
   * Retrieves the concrete appender from {@link #getAppender()}, wraps it in an {@link AsynchronousAppender}
   * with the configured buffer size and concurrency limit, registers a {@link DefaultTemplate} at the default
   * log level, and for each {@link Log} entry registers a {@link ClassNameTemplate} at the entry's level and pattern.
   *
   * @throws IOException if the concrete appender cannot be created
   * @throws Exception   if template registration or any other initialization step fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    Appender asynchronousAppender;

    asynchronousAppender = new AsynchronousAppender(getAppender(), logRecordBufferSize, concurrencyLimit);

    new DefaultTemplate(defaultLogLevel, true, asynchronousAppender).register();

    if ((logs != null)) {
      for (Log log : logs) {
        new ClassNameTemplate(log.getLevel(), true, log.getPattern(), asynchronousAppender).register();
      }
    }
  }
}
