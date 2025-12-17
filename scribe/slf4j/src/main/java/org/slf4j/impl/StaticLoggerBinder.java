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
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.slf4j.ScribeLoggerFactory;

/**
 * SLF4J binder that exposes the scribe-based {@link ILoggerFactory}.
 * This integrates the scribe logger implementation into the SLF4J discovery mechanism.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

  public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  private final ILoggerFactory loggerFactory;

  static {

    LoggerManager.addLoggingPackagePrefix("org.slf4j.");
  }

  /**
   * Creates a binder that delegates logger creation to {@link ScribeLoggerFactory}.
   */
  public StaticLoggerBinder () {

    loggerFactory = new ScribeLoggerFactory();
  }

  /**
   * Provides the singleton binder instance expected by SLF4J.
   *
   * @return the static binder
   */
  public static StaticLoggerBinder getSingleton () {

    return SINGLETON;
  }

  /**
   * Returns the factory that produces scribe-backed SLF4J loggers.
   *
   * @return the logger factory instance
   */
  public ILoggerFactory getLoggerFactory () {

    return loggerFactory;
  }

  /**
   * Returns the fully qualified class name of the bound logger factory.
   *
   * @return the logger factory class name
   */
  public String getLoggerFactoryClassStr () {

    return ScribeLoggerFactory.class.getName();
  }
}
