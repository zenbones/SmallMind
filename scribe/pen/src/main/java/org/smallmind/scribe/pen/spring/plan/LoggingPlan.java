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
 * Base Spring logging plan that wires an appender and templates based on provided patterns and default level.
 */
public abstract class LoggingPlan implements InitializingBean {

  private Log[] logs;
  private Level defaultLogLevel = Level.INFO;
  private int logRecordBufferSize = 400;
  private int concurrencyLimit = 1;

  /**
   * Provides the concrete appender for this plan.
   *
   * @return appender to use
   * @throws IOException if appender initialization fails
   */
  public abstract Appender getAppender ()
    throws IOException;

  /**
   * Sets the default log level applied to the root template.
   */
  public void setDefaultLogLevel (Level defaultLogLevel) {

    this.defaultLogLevel = defaultLogLevel;
  }

  /**
   * Sets the buffer size for the asynchronous wrapper.
   */
  public void setLogRecordBufferSize (int logRecordBufferSize) {

    this.logRecordBufferSize = logRecordBufferSize;
  }

  /**
   * Sets the concurrency limit for asynchronous workers.
   */
  public void setConcurrencyLimit (int concurrencyLimit) {

    this.concurrencyLimit = concurrencyLimit;
  }

  /**
   * Sets the specific logger patterns and levels to configure.
   */
  public void setLogs (Log[] logs) {

    this.logs = logs;
  }

  /**
   * Wraps the concrete appender asynchronously, registers a default template, and applies any specific logger mappings.
   *
   * @throws Exception if appender creation or template registration fails
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
