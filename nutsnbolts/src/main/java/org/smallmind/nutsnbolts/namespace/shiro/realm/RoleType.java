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
package org.smallmind.nutsnbolts.namespace.shiro.realm;

/**
 * Enumerates the roles an LDAP realm can grant. Each constant carries the string {@code code} that the realm places into
 * the Shiro {@link org.apache.shiro.authz.SimpleAuthorizationInfo} role set. Shiro itself attaches no meaning to role
 * strings — they only have to match the values used by authorization checks ({@code hasRole}, {@code @RequiresRoles},
 * {@code roles[...]}); this enum exists as the single, type-safe source for the role codes this package recognizes. A
 * realm derives a subject's roles by mapping the directory groups the subject belongs to onto these constants.
 */
public enum RoleType {

  ADMIN("ADMIN"),
  USER("USER"),
  GUEST("GUEST");

  private final String code;

  RoleType (String code) {

    this.code = code;
  }

  /**
   * Returns the role type whose {@link #getCode() code} equals the supplied string.
   *
   * @param code the role code to look up
   * @return the matching {@code RoleType}, or {@code null} if no constant carries that code
   */
  public static RoleType fromCode (String code) {

    for (RoleType roleType : values()) {
      if (roleType.code.equals(code)) {

        return roleType;
      }
    }

    return null;
  }

  /**
   * Returns the role code as it appears in a Shiro authorization role set.
   *
   * @return the string role code for this role type
   */
  public String getCode () {

    return code;
  }
}
