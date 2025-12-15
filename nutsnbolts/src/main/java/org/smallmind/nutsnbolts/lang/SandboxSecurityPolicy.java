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
 * {@link Policy} implementation that grants {@link AllPermission} only to whitelisted class loaders.
 * All other code sources receive a configurable base permission set.
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

  public SandboxSecurityPolicy (ClassLoader... whiteListedCompiledClassLoaders) {

    this(null, whiteListedCompiledClassLoaders);
  }

  /**
   * Creates a policy that whitelists both compiled and runtime class loader identities.
   *
   * @param whiteListedRuntimeClassLoaders  class names of runtime-generated loaders that should receive all permissions
   * @param whiteListedCompiledClassLoaders loader instances that should receive all permissions
   */
  public SandboxSecurityPolicy (String[] whiteListedRuntimeClassLoaders, ClassLoader... whiteListedCompiledClassLoaders) {

    whiteListedCompiledClassLoaderSet = ((whiteListedCompiledClassLoaders == null) || (whiteListedCompiledClassLoaders.length == 0)) ? Collections.emptySet() : new HashSet<>(Arrays.asList(whiteListedCompiledClassLoaders));
    whiteListedRuntimeClassLoaderSet = ((whiteListedRuntimeClassLoaders == null) || (whiteListedRuntimeClassLoaders.length == 0)) ? Collections.emptySet() : new HashSet<>(Arrays.asList(whiteListedRuntimeClassLoaders));
  }

  /**
   * Adds additional permissions to the base permission collection applied to non-whitelisted loaders.
   *
   * @param permissions permissions to add; ignored when {@code null} or empty
   * @return this policy instance for chaining
   */
  public SandboxSecurityPolicy addPermissions (Permission... permissions) {

    if ((permissions != null) && (permissions.length > 0)) {
      for (Permission permission : permissions) {
        basePermissionCollection.add(permission);
      }
    }

    return this;
  }

  @Override
  public PermissionCollection getPermissions (CodeSource codesource) {

    return new Permissions();
  }

  /**
   * Grants full permissions to whitelisted loaders and a shared base collection to all others.
   *
   * @param domain the protection domain being evaluated
   * @return the permission collection to apply
   */
  @Override
  public PermissionCollection getPermissions (ProtectionDomain domain) {

    return (whiteListedCompiledClassLoaderSet.contains(domain.getClassLoader()) || whiteListedRuntimeClassLoaderSet.contains(domain.getClassLoader().getClass().getName())) ? ALL_PERMISSION_COLLECTION : basePermissionCollection;
  }
}
