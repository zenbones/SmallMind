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
package org.smallmind.scribe.ink.log4j;

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class Log4JAppenderWrapper implements org.apache.logging.log4j.core.Appender {

  private final Appender appender;

  public Log4JAppenderWrapper (Appender appender) {

    this.appender = appender;
  }

  protected Appender getInnerAppender () {

    return appender;
  }

  @Override
  public String getName () {

    return appender.getName();
  }

  @Override
  public boolean ignoreExceptions () {

    return false;
  }

  @Override
  public State getState () {

    return State.STARTED;
  }

  @Override
  public void initialize () {

  }

  @Override
  public void start () {

  }

  @Override
  public void stop () {

  }

  @Override
  public boolean isStarted () {

    return true;
  }

  @Override
  public boolean isStopped () {

    return false;
  }

  @Override
  public void append (LogEvent logEvent) {

    if (appender.isActive()) {
      appender.publish(((RecordWrapper)logEvent).getRecord());
    }
  }

  @Override
  public ErrorHandler getHandler () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  @Override
  public void setHandler (ErrorHandler errorHandler) {

    appender.setErrorHandler(new Log4JErrorHandlerAdapter(errorHandler));
  }

  @Override
  public Layout<?> getLayout () {

    throw new UnsupportedOperationException("Unsupported native Log4J method");
  }

  @Override
  public int hashCode () {

    return appender.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    if (obj instanceof Log4JAppenderWrapper) {
      return appender.equals(((Log4JAppenderWrapper)obj).getInnerAppender());
    }

    return appender.equals(obj);
  }
}