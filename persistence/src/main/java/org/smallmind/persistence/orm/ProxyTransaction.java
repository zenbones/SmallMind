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

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.SnowflakeId;

/**
 * Wraps a native transaction, managing post-process hooks and rollback-only state.
 *
 * @param <S> the proxy session type that created this transaction
 */
public abstract class ProxyTransaction<S extends ProxySession<?, ?>> {

  private static final ProcessPriorityComparator PROCESS_PRIORITY_COMPARATOR = new ProcessPriorityComparator();

  private final S proxySession;
  private final String uuid;
  private LinkedList<TransactionPostProcess> postProcessList;
  private boolean rollbackOnly = false;

  /**
   * Creates a transaction wrapper bound to a session and assigns a unique id.
   *
   * @param proxySession the owning session
   */
  public ProxyTransaction (S proxySession) {

    this.proxySession = proxySession;

    uuid = SnowflakeId.newInstance().generateHexEncoding();
  }

  /**
   * Flushes the underlying session to persist any pending changes without committing.
   */
  public abstract void flush ();

  /**
   * Commits the underlying transaction.
   */
  public abstract void commit ();

  /**
   * Rolls back the underlying transaction.
   */
  public abstract void rollback ();

  /**
   * Indicates whether the transaction has completed.
   *
   * @return {@code true} if committed or rolled back
   */
  public abstract boolean isCompleted ();

  /**
   * @return the unique identifier for this transaction
   */
  public String getUniqueId () {

    return uuid;
  }

  /**
   * @return the owning proxy session
   */
  public S getSession () {

    return proxySession;
  }

  /**
   * Marks the transaction as rollback-only.
   */
  public void setRollbackOnly () {

    rollbackOnly = true;
  }

  /**
   * @return whether the transaction is flagged for rollback
   */
  public boolean isRollbackOnly () {

    return rollbackOnly;
  }

  /**
   * Lazily initializes the post-process list used to hold callbacks.
   *
   * @return list of post-process handlers
   */
  private LinkedList<TransactionPostProcess> getPostProcessList () {

    return (postProcessList == null) ? postProcessList = new LinkedList<>() : postProcessList;
  }

  /**
   * Adds a post-processing callback to run after completion.
   *
   * @param postProcess the callback to register
   */
  public void addPostProcess (TransactionPostProcess postProcess) {

    getPostProcessList().add(postProcess);
  }

  /**
   * Executes registered post-process callbacks appropriate for the end state.
   *
   * @param endState the transaction completion state
   * @throws TransactionPostProcessException if one or more callbacks fail
   */
  protected void applyPostProcesses (TransactionEndState endState)
    throws TransactionPostProcessException {

    TransactionPostProcessException postProcessException = null;

    getPostProcessList().sort(PROCESS_PRIORITY_COMPARATOR);
    for (TransactionPostProcess postProcess : getPostProcessList()) {
      if (postProcess.getEndState().equals(TransactionEndState.ANY) || postProcess.getEndState().equals(endState)) {
        try {
          postProcess.process();
        } catch (Exception exception) {
          if (postProcessException == null) {
            postProcessException = new TransactionPostProcessException(exception);
          } else {
            postProcessException.addSubsequentCause(exception);
          }
        }
      }
    }

    if (postProcessException != null) {
      throw postProcessException;
    }
  }

  /**
   * Generates a hash code based on the unique transaction id.
   *
   * @return hash code for this transaction wrapper
   */
  @Override
  public int hashCode () {

    return uuid.hashCode();
  }

  /**
   * Equality is based on the unique transaction id.
   *
   * @param obj object to compare
   * @return {@code true} when the ids match
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ProxyTransaction) && uuid.equals(((ProxyTransaction<?>)obj).getUniqueId());
  }
}
