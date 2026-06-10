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
package org.smallmind.quorum.namespace;

import java.util.Hashtable;
import javax.naming.CommunicationException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * Bridges the protected {@link PooledJavaContext} nested constructor — only reachable from inside the
 * {@code org.smallmind.quorum.namespace} package — out to tests in sibling packages (notably
 * {@code …namespace.pool}). It builds a pooled context over a {@link ControllableBackingContext} whose
 * {@code lookup} can be programmed to throw, so the pooled context's communication-abort path and a
 * component instance's reaction to it can be driven without a live backing store.
 */
public final class PooledJavaContextTestKit {

  private PooledJavaContextTestKit () {

  }

  /**
   * Builds a pooled context wrapping the supplied controllable backing context, using a stub
   * translator that needs no live connection.
   *
   * @param backing    the programmable backing directory context to wrap
   * @param modifiable {@code true} to allow mutations on the pooled context
   * @return a pooled context suitable for driving lifecycle and abort behaviour in a test
   */
  public static PooledJavaContext pooledContextOver (ControllableBackingContext backing, boolean modifiable) {

    NamespaceTestSupport.StubNameTranslator translator = new NamespaceTestSupport.StubNameTranslator();

    return new PooledJavaContext(new Hashtable<>(), backing, translator, new JavaNameParser(translator), modifiable);
  }

  /**
   * A backing {@link javax.naming.directory.DirContext} whose {@code lookup} can be told to succeed,
   * to fail with a recoverable {@link NameNotFoundException}, or to fail with a
   * {@link CommunicationException} that triggers the pooled context's abort path. Physical closes are
   * counted so idempotency can be asserted.
   */
  public static final class ControllableBackingContext extends NamespaceTestSupport.RecordingDirContext {

    private LookupBehavior lookupBehavior = LookupBehavior.SUCCEED;

    /**
     * Sets how the next {@code lookup} call behaves.
     *
     * @param lookupBehavior the behaviour to apply
     */
    public void setLookupBehavior (LookupBehavior lookupBehavior) {

      this.lookupBehavior = lookupBehavior;
    }

    /**
     * Returns the number of times this backing context has been physically closed.
     *
     * @return the close count
     */
    public int closes () {

      return getCloseCount();
    }

    @Override
    public Object lookup (Name name)
      throws NamingException {

      switch (lookupBehavior) {
        case THROW_COMMUNICATION:
          throw new CommunicationException("backing store is unreachable");
        case THROW_NAMING:
          throw new NameNotFoundException("no such entry");
        default:
          return super.lookup(name);
      }
    }
  }

  /**
   * The three programmable outcomes of a backing-context {@code lookup}.
   */
  public enum LookupBehavior {

    SUCCEED, THROW_NAMING, THROW_COMMUNICATION
  }
}
