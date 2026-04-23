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
package org.smallmind.spark.tanukisoft.maven;

/**
 * Coarse classification of wrapper platforms into the two families ({@code WINDOWS} and {@code UNIX}) that determine
 * which scripts and native library name the generator produces. Each constant carries the platform's
 * generically-named library.
 */
public enum OSStyle {

  WINDOWS("wrapper.dll"), UNIX("libwrapper.so");

  private final String library;

  /**
   * Associates the style with the generic native library name it installs alongside the platform-specific one.
   *
   * @param library the generic library filename for this style
   */
  OSStyle (String library) {

    this.library = library;
  }

  /**
   * Returns the generic wrapper library filename associated with this style.
   *
   * @return the generic library filename
   */
  public String getLibrary () {

    return library;
  }
}
