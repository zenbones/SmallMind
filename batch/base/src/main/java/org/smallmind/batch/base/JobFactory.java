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
package org.smallmind.batch.base;

import java.util.Map;

/**
 * Contract for creating and restarting batch jobs without coupling callers to a specific
 * batch framework.
 * <p>
 * Implementations translate the framework-neutral {@link BatchParameter} wrappers into
 * whatever representation the underlying engine requires.
 */
public interface JobFactory {

  /**
   * Creates a new job execution and returns its identifier.
   *
   * @param logicalName  name used to look up the job definition
   * @param parameterMap job parameters keyed by name; may be {@code null} or empty
   * @param reason       optional description of why the job is being started; used only for
   *                     logging or auditing and may be {@code null}
   * @return the unique execution id assigned to the newly launched job
   * @throws Exception if the job cannot be located, parameters are invalid, or the execution
   *                   cannot be started
   */
  Long create (String logicalName, Map<String, BatchParameter<?>> parameterMap, String reason)
    throws Exception;

  /**
   * Re-runs a previously executed job identified by its execution id.
   *
   * @param executionId the id of the job execution to restart
   * @throws Exception if the execution cannot be found or the job does not support restart
   */
  void restart (long executionId)
    throws Exception;
}
