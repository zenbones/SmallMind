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
package org.smallmind.liquibase.spring;

import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogService;

/**
 * Liquibase {@code LogService} that integrates the SmallMind Scribe logging system.
 *
 * <p>Liquibase discovers log service implementations via the Java {@link java.util.ServiceLoader}
 * mechanism. This class must therefore be listed in
 * {@code META-INF/services/liquibase.logging.LogService} on the classpath for Liquibase to pick
 * it up automatically.</p>
 *
 * <p>A priority one above {@code PRIORITY_DEFAULT} ensures this service is preferred over
 * Liquibase's built-in JUL service when both are present on the classpath.</p>
 */
public class ScribeLogService extends AbstractLogService {

  /**
   * Returns the selection priority for this log service.
   *
   * <p>Liquibase chooses the service with the highest priority when multiple implementations
   * are available. Returning {@code PRIORITY_DEFAULT + 1} guarantees this service wins over
   * Liquibase's built-in default without conflicting with other custom services that explicitly
   * claim a higher priority.</p>
   *
   * @return {@code PRIORITY_DEFAULT + 1}
   */
  @Override
  public int getPriority () {

    return PRIORITY_DEFAULT + 1;
  }

  /**
   * Creates a {@link ScribeLiquibaseLogger} bound to the requesting class.
   *
   * <p>A new logger instance is returned on every call; callers should not cache the result
   * beyond the scope of the object that requested it, as the underlying Scribe logger is
   * already keyed by class.</p>
   *
   * @param clazz the class for which a logger is required; must not be {@code null}
   * @return a new {@link ScribeLiquibaseLogger} that routes Liquibase log events for
   * {@code clazz} through the Scribe logging system
   */
  @Override
  public Logger getLog (Class clazz) {

    return new ScribeLiquibaseLogger(clazz);
  }
}
