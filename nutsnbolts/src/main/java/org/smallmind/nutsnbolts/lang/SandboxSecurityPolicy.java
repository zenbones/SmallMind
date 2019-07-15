/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.security.util.SecurityConstants;

/*
    Policy.setPolicy(new SandboxSecurityPolicy(<White Listed ClassLoader(s)>));
    System.setSecurityManager(new SecurityManager());
*/
public class SandboxSecurityPolicy extends Policy {

  private static final PermissionCollection ALL_PERMISSION_COLLECTION;
  private Set<? extends ClassLoader> whiteListedClassLoaderSet;
  private PermissionCollection basePermissionCollection = new Permissions();

  static {

    ALL_PERMISSION_COLLECTION = new AllPermission().newPermissionCollection();
    ALL_PERMISSION_COLLECTION.add(SecurityConstants.ALL_PERMISSION);
  }

  public SandboxSecurityPolicy (ClassLoader... whiteListedClassLoaders) {

    whiteListedClassLoaderSet = ((whiteListedClassLoaders == null) || (whiteListedClassLoaders.length == 0)) ? Collections.emptySet() : new HashSet<>(Arrays.asList(whiteListedClassLoaders));
  }

  public SandboxSecurityPolicy addPermissions (Permission... permissions) {

    if ((permissions != null) && (permissions.length > 0)) {
      for (Permission permission : permissions) {
        basePermissionCollection.add(permission);
      }
    }

    return this;
  }

  @Override
  public PermissionCollection getPermissions (ProtectionDomain domain) {

    return whiteListedClassLoaderSet.contains(domain.getClassLoader()) ? ALL_PERMISSION_COLLECTION : basePermissionCollection;
  }
}
