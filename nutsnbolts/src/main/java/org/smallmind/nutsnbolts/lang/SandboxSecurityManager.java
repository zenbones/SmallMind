/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.security.AccessControlContext;
import java.security.Permission;
import sun.security.util.SecurityConstants;

/*
    System.setSecurityManager(new SandboxSecurityManager(((currentSecurityManager = System.getSecurityManager()) == null) ? new SecurityManager() : currentSecurityManager));
*/

public class SandboxSecurityManager extends SecurityManager {

  private static ThreadLocal<Boolean> CHECK_IN_PROGRESS_THREAD_LOCAL = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private SecurityManager securityManager;
  private Permission sandboxPermission;

  public SandboxSecurityManager (SecurityManager securityManager) {

    this(securityManager, new SandboxPermission());
  }

  public SandboxSecurityManager (SecurityManager securityManager, Permission sandboxPermission) {

    this.securityManager = securityManager;
    this.sandboxPermission = sandboxPermission;
  }

  @Override
  public void checkPermission (Permission perm) {

    if ((!CHECK_IN_PROGRESS_THREAD_LOCAL.get()) && impliesSandbox()) {
      securityManager.checkPermission(perm);
    }
  }

  @Override
  public void checkPermission (Permission perm, Object context) {

    if (context instanceof AccessControlContext) {
      if ((!CHECK_IN_PROGRESS_THREAD_LOCAL.get()) && impliesSandbox()) {
        securityManager.checkPermission(perm, context);
      }
    } else {
      securityManager.checkPermission(perm, context);
    }
  }

  private boolean impliesSandbox () {

    CHECK_IN_PROGRESS_THREAD_LOCAL.set(Boolean.TRUE);
    try {
      for (Class<?> clazz : getClassContext()) {
        if ((!clazz.getProtectionDomain().implies(SecurityConstants.ALL_PERMISSION)) && clazz.getProtectionDomain().implies(sandboxPermission)) {

          return true;
        }
      }

      return false;
    } finally {
      CHECK_IN_PROGRESS_THREAD_LOCAL.set(false);
    }
  }
}
