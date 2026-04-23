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
package org.smallmind.persistence.orm.aop;

/**
 * Non-terminal {@link TransactionError} raised when a commit or rollback fails mid-boundary,
 * allowing the aspect to continue unwinding remaining transactions before rethrowing.
 */
public class IncompleteTransactionError extends TransactionError {

  /**
   * Creates an error wrapping the transactional failure that interrupted boundary processing.
   *
   * @param throwable the cause of the incomplete transaction
   */
  public IncompleteTransactionError (Throwable throwable) {

    super(throwable);
  }

  /**
   * Returns {@code false} so that boundary logic continues processing remaining transactions.
   *
   * @return {@code false}
   */
  @Override
  public boolean isTerminal () {

    return false;
  }
}
