/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

public abstract class ProxyTransaction<S extends ProxySession> {

  private static final ProcessPriorityComparator PROCESS_PRIORITY_COMPARATOR = new ProcessPriorityComparator();

  private S proxySession;
  private LinkedList<TransactionPostProcess> postProcessList;
  private String uuidAsString;
  private boolean rollbackOnly = false;

  public ProxyTransaction (S proxySession) {

    this.proxySession = proxySession;

    uuidAsString = UUID.randomUUID().toString();
  }

  public abstract void flush ();

  public abstract void commit ();

  public abstract void rollback ();

  public abstract boolean isCompleted ();

  public String getUniqueId () {

    return uuidAsString;
  }

  public S getSession () {

    return proxySession;
  }

  public void setRollbackOnly () {

    rollbackOnly = true;
  }

  public boolean isRollbackOnly () {

    return rollbackOnly;
  }

  private LinkedList<TransactionPostProcess> getPostProcessList () {

    return (postProcessList == null) ? postProcessList = new LinkedList<TransactionPostProcess>() : postProcessList;
  }

  public void addPostProcess (TransactionPostProcess postProcess) {

    getPostProcessList().add(postProcess);
  }

  protected void applyPostProcesses (TransactionEndState endState)
    throws TransactionPostProcessException {

    TransactionPostProcessException postProcessException = null;

    Collections.sort(getPostProcessList(), PROCESS_PRIORITY_COMPARATOR);
    for (TransactionPostProcess postProcess : getPostProcessList()) {
      if (postProcess.getEndState().equals(TransactionEndState.ANY) || postProcess.getEndState().equals(endState)) {
        try {
          postProcess.process();
        }
        catch (Exception exception) {
          if (postProcessException == null) {
            postProcessException = new TransactionPostProcessException(exception);
          }
          else {
            postProcessException.addSubsequentCause(exception);
          }
        }
      }
    }

    if (postProcessException != null) {
      throw postProcessException;
    }
  }

  public int hashCode () {

    return uuidAsString.hashCode();
  }

  public boolean equals (Object obj) {

    return (obj instanceof ProxyTransaction) && uuidAsString.equals(((ProxyTransaction)obj).getUniqueId());
  }
}
