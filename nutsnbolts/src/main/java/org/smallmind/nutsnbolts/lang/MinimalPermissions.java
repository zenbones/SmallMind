/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.net.SocketPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.PropertyPermission;

public class MinimalPermissions extends PermissionCollection {

  private final Permissions permissions = new Permissions();

  public MinimalPermissions () {

    permissions.add(new SocketPermission("localhost:0", "listen"));

    permissions.add(new PropertyPermission("java.version", "read"));
    permissions.add(new PropertyPermission("java.vendor", "read"));
    permissions.add(new PropertyPermission("java.vendor.url", "read"));
    permissions.add(new PropertyPermission("java.class.version", "read"));
    permissions.add(new PropertyPermission("os.name", "read"));
    permissions.add(new PropertyPermission("os.version", "read"));
    permissions.add(new PropertyPermission("os.arch", "read"));
    permissions.add(new PropertyPermission("file.separator", "read"));
    permissions.add(new PropertyPermission("path.separator", "read"));
    permissions.add(new PropertyPermission("line.separator", "read"));

    permissions.add(new PropertyPermission("java.runtime.name", "read"));

    permissions.add(new PropertyPermission("java.specification.version", "read"));
    permissions.add(new PropertyPermission("java.specification.vendor", "read"));
    permissions.add(new PropertyPermission("java.specification.name", "read"));

    permissions.add(new PropertyPermission("java.vm.specification.version", "read"));
    permissions.add(new PropertyPermission("java.vm.specification.vendor", "read"));
    permissions.add(new PropertyPermission("java.vm.specification.name", "read"));
    permissions.add(new PropertyPermission("java.vm.version", "read"));
    permissions.add(new PropertyPermission("java.vm.vendor", "read"));
    permissions.add(new PropertyPermission("java.vm.name", "read"));
  }

  @Override
  public void add (Permission permission) {

    permissions.add(permission);
  }

  @Override
  public boolean implies (Permission permission) {

    return permissions.implies(permission);
  }

  @Override
  public Enumeration<Permission> elements () {

    return permissions.elements();
  }
}
