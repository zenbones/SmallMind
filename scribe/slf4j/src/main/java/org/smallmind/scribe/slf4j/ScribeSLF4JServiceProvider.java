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
package org.smallmind.scribe.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * SLF4J 2.0 {@link SLF4JServiceProvider} entry point that installs Scribe as the SLF4J
 * backend. Discovered via {@code META-INF/services/org.slf4j.spi.SLF4JServiceProvider};
 * once on the classpath no further configuration is needed to route
 * {@code org.slf4j.Logger} calls through the Scribe pipeline.
 *
 * <p>The static initialiser registers {@code "org.slf4j."} as a logging package prefix
 * so that scribe's call-site stack walk skips SLF4J and adapter frames when computing
 * caller location information.
 */
public class ScribeSLF4JServiceProvider implements SLF4JServiceProvider {

  private final ILoggerFactory loggerFactory;
  private final MDCAdapter mdcAdapter;
  private final IMarkerFactory markerFactory;

  static {

    LoggerManager.addLoggingPackagePrefix("org.slf4j.");
  }

  /**
   * Constructs the provider, eagerly instantiating {@link ScribeLoggerFactory},
   * {@link ScribeMDCAdapter}, and {@link ScribeMarkerFactory}.
   */
  public ScribeSLF4JServiceProvider () {

    loggerFactory = new ScribeLoggerFactory();
    mdcAdapter = new ScribeMDCAdapter();
    markerFactory = new ScribeMarkerFactory();
  }

  /**
   * Returns the SLF4J API version this provider was compiled against.
   *
   * @return {@code "2.0.1"}
   */
  @Override
  public String getRequestedApiVersion () {

    return "2.0.1";
  }

  /**
   * No-op; all factories are initialised in the constructor.
   */
  @Override
  public void initialize () {

  }

  /**
   * Returns the {@link ScribeLoggerFactory} that maps SLF4J logger names to scribe loggers.
   *
   * @return the shared {@link ILoggerFactory} instance
   */
  @Override
  public ILoggerFactory getLoggerFactory () {

    return loggerFactory;
  }

  /**
   * Returns the stub {@link ScribeMarkerFactory}; Scribe does not support SLF4J markers.
   *
   * @return the shared {@link IMarkerFactory} instance
   */
  @Override
  public IMarkerFactory getMarkerFactory () {

    return markerFactory;
  }

  /**
   * Returns the {@link ScribeMDCAdapter} that delegates MDC writes to the scribe
   * thread-local parameter store.
   *
   * @return the shared {@link MDCAdapter} instance
   */
  @Override
  public MDCAdapter getMDCAdapter () {

    return mdcAdapter;
  }
}
