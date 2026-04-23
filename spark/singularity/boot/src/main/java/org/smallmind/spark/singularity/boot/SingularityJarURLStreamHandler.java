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
package org.smallmind.spark.singularity.boot;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stream handler registered for the synthetic {@code singularity} protocol. It produces a
 * {@link SingularityJarURLConnection} capable of resolving a specific entry inside a nested jar that lives
 * within a Singularity bundle.
 */
public class SingularityJarURLStreamHandler extends URLStreamHandler {

  /**
   * Builds the connection used to read the resource addressed by a {@code singularity:} URL.
   *
   * @param url a URL whose protocol is {@code singularity} and whose path encodes both the outer jar and the
   *            inner entry to resolve
   * @return a fresh {@link SingularityJarURLConnection} bound to the supplied URL
   */
  @Override
  protected URLConnection openConnection (URL url) {

    return new SingularityJarURLConnection(url);
  }
}
