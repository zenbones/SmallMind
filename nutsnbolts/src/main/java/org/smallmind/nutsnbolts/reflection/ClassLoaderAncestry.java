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
package org.smallmind.nutsnbolts.reflection;

/**
 * Compares class loaders by their parent chains, so that a cache can tell when a class it would hold an entry
 * against might outlive the classes the entry refers to.
 */
final class ClassLoaderAncestry {

  private ClassLoaderAncestry () {

  }

  /**
   * Determines whether one class loader is a strict ancestor of another through the parent chain. The bootstrap
   * loader, represented by {@code null}, is an ancestor of every other loader.
   *
   * @param candidateLoader the loader that might be the ancestor, or {@code null} for the bootstrap loader
   * @param loader          the loader whose parents are searched, or {@code null} for the bootstrap loader
   * @return {@code true} if {@code candidateLoader} is a parent, grandparent, or more distant parent of
   * {@code loader}
   */
  static boolean isStrictAncestor (ClassLoader candidateLoader, ClassLoader loader) {

    ClassLoader currentLoader = loader;

    if (currentLoader == null) {

      return false;
    } else if (candidateLoader == null) {

      return true;
    }

    while ((currentLoader = currentLoader.getParent()) != null) {
      if (currentLoader == candidateLoader) {

        return true;
      }
    }

    return false;
  }
}
