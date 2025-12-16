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
package org.smallmind.persistence.orm;

/**
 * Defines a callback to be invoked after a transaction completes.
 */
public abstract class TransactionPostProcess {

  private final TransactionEndState endState;
  private final ProcessPriority priority;

  /**
   * Creates a post-process callback with an end-state filter and priority.
   *
   * @param endState the transaction completion state in which this callback should run
   * @param priority the ordering priority for execution
   */
  public TransactionPostProcess (TransactionEndState endState, ProcessPriority priority) {

    this.endState = endState;
    this.priority = priority;
  }

  /**
   * Executes the post-process logic.
   *
   * @throws Exception if the callback fails
   */
  public abstract void process ()
    throws Exception;

  /**
   * @return the transaction end state for which this callback is applicable
   */
  public TransactionEndState getEndState () {

    return endState;
  }

  /**
   * @return the execution priority for this callback
   */
  public ProcessPriority getPriority () {

    return priority;
  }
}
