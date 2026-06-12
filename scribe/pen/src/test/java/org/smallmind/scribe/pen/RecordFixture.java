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
package org.smallmind.scribe.pen;

/**
 * A mutable, hand-rolled {@link Record} used by the filter and formatter tests. Constructing a real
 * record requires the active {@code LoggingBlueprint}, which is a backend (classpath) decision that
 * unit tests deliberately avoid; this fixture lets a test populate exactly the fields the code under
 * test reads, with fluent setters, and returns sensible non-null defaults for everything else
 * (notably {@link #getParameters()}, which the contract promises is never {@code null}).
 */
public class RecordFixture implements Record<Object> {

  private Object nativeLogEntry;
  private LoggerContext loggerContext;
  private Level level = Level.INFO;
  private Parameter[] parameters = new Parameter[0];
  private Throwable thrown;
  private String loggerName = "test.Logger";
  private String message;
  private String threadName = "test-thread";
  private long threadID;
  private long sequenceNumber;
  private long millis;

  public RecordFixture setLevel (Level level) {

    this.level = level;

    return this;
  }

  public RecordFixture setLoggerName (String loggerName) {

    this.loggerName = loggerName;

    return this;
  }

  public RecordFixture setMessage (String message) {

    this.message = message;

    return this;
  }

  public RecordFixture setThrown (Throwable thrown) {

    this.thrown = thrown;

    return this;
  }

  public RecordFixture setParameters (Parameter[] parameters) {

    this.parameters = parameters;

    return this;
  }

  public RecordFixture setLoggerContext (LoggerContext loggerContext) {

    this.loggerContext = loggerContext;

    return this;
  }

  public RecordFixture setThreadName (String threadName) {

    this.threadName = threadName;

    return this;
  }

  public RecordFixture setMillis (long millis) {

    this.millis = millis;

    return this;
  }

  @Override
  public Object getNativeLogEntry () {

    return nativeLogEntry;
  }

  @Override
  public String getLoggerName () {

    return loggerName;
  }

  @Override
  public Level getLevel () {

    return level;
  }

  @Override
  public Throwable getThrown () {

    return thrown;
  }

  @Override
  public String getMessage () {

    return message;
  }

  @Override
  public Parameter[] getParameters () {

    return parameters;
  }

  @Override
  public LoggerContext getLoggerContext () {

    return loggerContext;
  }

  @Override
  public long getThreadID () {

    return threadID;
  }

  @Override
  public String getThreadName () {

    return threadName;
  }

  @Override
  public long getSequenceNumber () {

    return sequenceNumber;
  }

  @Override
  public long getMillis () {

    return millis;
  }
}
