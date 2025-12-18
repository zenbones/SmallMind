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
package org.smallmind.web.jersey.json;

import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.web.jersey.spring.PrioritizedResourceConfigExtension;

/**
 * Resource configuration extension that installs {@link ThrowableExceptionMapper} with optional custom mappers.
 */
public class ThrowableExceptionExtension extends PrioritizedResourceConfigExtension {

  private ExceptionMapper[] mappers;
  private boolean logUnclassifiedErrors = false;

  /**
   * Sets additional exception mappers that should be consulted before the default mapping occurs.
   *
   * @param mappers mapper instances
   */
  public void setMappers (ExceptionMapper[] mappers) {

    this.mappers = mappers;
  }

  /**
   * Controls whether previously unclassified errors should be logged.
   *
   * @param logUnclassifiedErrors {@code true} to log unhandled exceptions
   */
  public void setLogUnclassifiedErrors (boolean logUnclassifiedErrors) {

    this.logUnclassifiedErrors = logUnclassifiedErrors;
  }

  /**
   * Registers the {@link ThrowableExceptionMapper} and sets Jersey properties needed for status handling.
   *
   * @param resourceConfig Jersey resource configuration
   */
  @Override
  public void apply (ResourceConfig resourceConfig) {

    resourceConfig.property("jersey.config.server.response.setStatusOverSendError", "true");
    resourceConfig.register(new ThrowableExceptionMapper(logUnclassifiedErrors, mappers), getPriority());
  }
}
