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

/**
 * Enumeration of supported backing store technologies for the {@code java:} namespace.
 * <p>
 * Each constant carries the short identifier string used in environment configuration entries
 * when selecting the {@link ContextCreator} and {@link NameTranslator} implementations for a
 * given {@link org.smallmind.quorum.namespace.JavaContext}.
 */
public enum StorageType {

  /**
   * LDAP directory service, using {@code com.sun.jndi.ldap.LdapCtxFactory}.
   */
  LDAP("ldap");

  private final String backingStore;

  /**
   * Associates the enum constant with its configuration identifier string.
   *
   * @param backingStore the short identifier used to select this storage type in configuration
   */
  StorageType (String backingStore) {

    this.backingStore = backingStore;
  }

  /**
   * Returns the short identifier string for this backing store type.
   *
   * @return the configuration identifier; never {@code null}
   */
  public String getBackingStore () {

    return backingStore;
  }
}
