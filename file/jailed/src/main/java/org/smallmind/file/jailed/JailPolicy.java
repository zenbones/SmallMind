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
package org.smallmind.file.jailed;

import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Determines how rigorously a {@link JailedPathTranslator} enforces the jail boundary when a
 * jailed path is translated into its native equivalent.
 *
 * <p>Both policies apply the same structural confinement: the jailed path is normalized with
 * {@code ..} clamped at the jail root, every name element is validated as a single legal name
 * on the native file system, and the assembled native path is verified to lie beneath the jail
 * root. The policies differ only in whether the <em>real</em> (symbolic-link resolved) location
 * of the native path is also verified.
 *
 * @see AbstractJailedPathTranslator
 */
public enum JailPolicy {

  /**
   * Structural confinement only, with no additional file-system access.
   *
   * <p>Translation performs no I/O, which makes it the cheaper of the two policies, but a
   * symbolic link (or, on Windows, a junction or other reparse point) that lives inside the
   * jail and points outside of it will be followed out of the jail by the native file system.
   * Choose this policy only when the contents of the jail are fully trusted, or when the native
   * file system has no notion of links at all.
   */
  LENIENT,

  /**
   * Structural confinement plus verification that the real location of the native path remains
   * inside the jail.
   *
   * <p>The deepest existing ancestor of the translated native path is resolved with
   * {@link Path#toRealPath(LinkOption...)} and compared against the real path of the jail root,
   * so a link pointing out of the jail is refused rather than followed. Resolving both sides
   * also means that a jail root which is itself reached through a link (for example a
   * {@code /tmp} that is a link to {@code /private/tmp}, or a Windows short-name alias) is
   * still recognized as the jail root.
   *
   * <p>When the caller passes {@link LinkOption#NOFOLLOW_LINKS} the final name element is
   * excluded from the check, so an escaping link may still be deleted or have its own
   * attributes read - only reading <em>through</em> it is refused.
   *
   * <p>This is the default policy, at the cost of one or two extra file-system lookups per
   * translated path.
   *
   * <p>Verification necessarily happens before the operation it guards, so a link substituted
   * in between the two would not be caught - a race inherent to confining by path rather than
   * by descriptor, and one that can only be closed by the operating system. What this policy
   * does guarantee is that a link already in place is not followed out of the jail.
   */
  STRICT
}
