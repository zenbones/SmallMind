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
package org.smallmind.nutsnbolts.lang;

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
    Policy.setPolicy(new SandboxSecurityPolicy(<White Listed ClassLoader(s)>));
    System.setSecurityManager(new SecurityManager());
*/

/**
 * {@link Policy} that grants {@link AllPermission} only to explicitly whitelisted class loaders
 * and applies a configurable minimal permission set to all other code.
 */
public class SandboxSecurityPolicy extends Policy {

  private static final PermissionCollection ALL_PERMISSION_COLLECTION;
  private final Set<? extends ClassLoader> whiteListedCompiledClassLoaderSet;
  private final Set<String> whiteListedRuntimeClassLoaderSet;
  private final PermissionCollection basePermissionCollection = new Permissions();

  static {

    AllPermission allPermission = new AllPermission();

    ALL_PERMISSION_COLLECTION = allPermission.newPermissionCollection();
    ALL_PERMISSION_COLLECTION.add(allPermission);
  }

  /**
   * Constructs a policy that whitelists the given compiled class loaders and assigns all other code
   * the default empty base permission set.
   *
   * @param whiteListedCompiledClassLoaders loader instances that should receive {@link AllPermission}
   */
  public SandboxSecurityPolicy (ClassLoader... whiteListedCompiledClassLoaders) {

    this(null, whiteListedCompiledClassLoaders);
  }

  /**
   * Constructs a policy that whitelists both statically known loader instances and loaders identified
   * by class name at runtime, granting all permissions to matched loaders and the base set to all others.
   *
   * @param whiteListedRuntimeClassLoaders  fully-qualified class names of dynamically generated loaders
   *                                        that should receive {@link AllPermission}
   * @param whiteListedCompiledClassLoaders loader instances that should receive {@link AllPermission}
   */
  public SandboxSecurityPolicy (String[] whiteListedRuntimeClassLoaders, ClassLoader... whiteListedCompiledClassLoaders) {

    whiteListedCompiledClassLoaderSet = ((whiteListedCompiledClassLoaders == null) || (whiteListedCompiledClassLoaders.length == 0)) ? Collections.emptySet() : new HashSet<>(Arrays.asList(whiteListedCompiledClassLoaders));
    whiteListedRuntimeClassLoaderSet = ((whiteListedRuntimeClassLoaders == null) || (whiteListedRuntimeClassLoaders.length == 0)) ? Collections.emptySet() : new HashSet<>(Arrays.asList(whiteListedRuntimeClassLoaders));
  }

  /**
   * Adds the given permissions to the base collection that is granted to non-whitelisted class loaders,
   * returning this policy to support a fluent configuration style.
   *
   * @param permissions the permissions to add; a {@code null} or empty array is silently ignored
   * @return this policy instance
   */
  public SandboxSecurityPolicy addPermissions (Permission... permissions) {

    if ((permissions != null) && (permissions.length > 0)) {
      for (Permission permission : permissions) {
        basePermissionCollection.add(permission);
      }
    }

    return this;
  }

  /**
   * Returns an empty permission collection for the given code source; domain-level evaluation is
   * performed by {@link #getPermissions(ProtectionDomain)}.
   *
   * @param codesource the code source being evaluated
   * @return an empty {@link Permissions} collection
   */
  @Override
  public PermissionCollection getPermissions (CodeSource codesource) {

    return new Permissions();
  }

  /**
   * Returns {@link AllPermission} for domains whose loader is whitelisted, and the base permission
   * collection for all other domains.
   *
   * @param domain the protection domain being evaluated
   * @return the applicable permission collection
   */
  @Override
  public PermissionCollection getPermissions (ProtectionDomain domain) {

    return (whiteListedCompiledClassLoaderSet.contains(domain.getClassLoader()) || whiteListedRuntimeClassLoaderSet.contains(domain.getClassLoader().getClass().getName())) ? ALL_PERMISSION_COLLECTION : basePermissionCollection;
  }
}
