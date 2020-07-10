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
package org.smallmind.scribe.apache;

import org.apache.commons.logging.Log;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public class CommonsLogWrapper implements Log {

  private final String name;

  static {

    LoggerManager.addLoggingPackagePrefix("org.apache.commons.logging.");
  }

  public CommonsLogWrapper (String name) {

    this.name = name;
  }

  private Logger getLogger () {

    return LoggerManager.getLogger(name);
  }

  public boolean isDebugEnabled () {

    return getLogger().getLevel().noGreater(Level.DEBUG);
  }

  public boolean isErrorEnabled () {

    return getLogger().getLevel().noGreater(Level.ERROR);
  }

  public boolean isFatalEnabled () {

    return getLogger().getLevel().noGreater(Level.FATAL);
  }

  public boolean isInfoEnabled () {

    return getLogger().getLevel().noGreater(Level.INFO);
  }

  public boolean isTraceEnabled () {

    return getLogger().getLevel().noGreater(Level.TRACE);
  }

  public boolean isWarnEnabled () {

    return getLogger().getLevel().noGreater(Level.WARN);
  }

  public void trace (Object o) {

    getLogger().trace(o);
  }

  public void trace (Object o, Throwable throwable) {

    getLogger().trace(throwable, o);
  }

  public void debug (Object o) {

    getLogger().debug(o);
  }

  public void debug (Object o, Throwable throwable) {

    getLogger().debug(throwable, o);
  }

  public void info (Object o) {

    getLogger().info(o);
  }

  public void info (Object o, Throwable throwable) {

    getLogger().info(throwable, o);
  }

  public void warn (Object o) {

    getLogger().warn(o);
  }

  public void warn (Object o, Throwable throwable) {

    getLogger().warn(throwable, o);
  }

  public void error (Object o) {

    getLogger().error(o);
  }

  public void error (Object o, Throwable throwable) {

    getLogger().error(throwable, o);
  }

  public void fatal (Object o) {

    getLogger().fatal(o);
  }

  public void fatal (Object o, Throwable throwable) {

    getLogger().fatal(throwable, o);
  }
}