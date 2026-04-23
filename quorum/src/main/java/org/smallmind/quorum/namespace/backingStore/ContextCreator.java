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
package org.smallmind.quorum.namespace.backingStore;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Abstract factory that creates the initial {@link DirContext} used to connect to a naming backing store.
 * <p>
 * Subclasses implement {@link #getInitialContext()} for a specific backing store technology (for example,
 * LDAP) using the {@link NamingConnectionDetails} provided at construction time. The resulting context
 * is passed to {@link NameTranslator} implementations so that names can be resolved against the store.
 */
public abstract class ContextCreator {

  private final NamingConnectionDetails connectionDetails;

  /**
   * Constructs a creator that will use the supplied connection details when opening contexts.
   *
   * @param connectionDetails host, port, credentials, and root namespace for the backing store
   */
  public ContextCreator (NamingConnectionDetails connectionDetails) {

    this.connectionDetails = connectionDetails;
  }

  /**
   * Returns the connection details held by this creator.
   *
   * @return the {@link NamingConnectionDetails} supplied at construction time; never {@code null}
   */
  public NamingConnectionDetails getConnectionDetails () {

    return connectionDetails;
  }

  /**
   * Opens and returns a new initial directory context for the configured backing store.
   * <p>
   * Each call may create a new physical connection; callers are responsible for closing the returned
   * context when it is no longer needed.
   *
   * @return a live {@link DirContext} bound to the backing store
   * @throws NamingException if the backing store cannot be reached or authentication fails
   */
  public abstract DirContext getInitialContext ()
    throws NamingException;
}
