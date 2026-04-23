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
 * SLF4J {@link LoggerFactoryBinder} placed in the {@code org.slf4j.impl} package to satisfy
 * the SLF4J 1.x static binding contract; routes all SLF4J logger requests to the scribe
 * framework via {@link ScribeLoggerFactory} and registers {@code "org.slf4j."} as a logging
 * package prefix so that call-site resolution skips SLF4J frames.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

  public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  private final ILoggerFactory loggerFactory;

  static {

    LoggerManager.addLoggingPackagePrefix("org.slf4j.");
  }

  /**
   * Constructs the binder and initialises the {@link ScribeLoggerFactory} that will
   * serve all subsequent {@link #getLoggerFactory()} calls.
   */
  public StaticLoggerBinder () {

    loggerFactory = new ScribeLoggerFactory();
  }

  /**
   * Returns the process-wide singleton required by the SLF4J static binding mechanism.
   *
   * @return the single {@code StaticLoggerBinder} instance
   */
  public static StaticLoggerBinder getSingleton () {

    return SINGLETON;
  }

  /**
   * Returns the {@link ILoggerFactory} that creates scribe-backed {@link org.slf4j.Logger} adapters.
   *
   * @return the logger factory bound to this implementation
   */
  public ILoggerFactory getLoggerFactory () {

    return loggerFactory;
  }

  /**
   * Reports the fully-qualified class name of the bound factory for SLF4J internal diagnostics.
   *
   * @return the canonical name of {@link ScribeLoggerFactory}
   */
  public String getLoggerFactoryClassStr () {

    return ScribeLoggerFactory.class.getName();
  }
}
