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
package org.smallmind.spark.singularity.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.smallmind.spark.singularity.dep.DepGreeter;

/**
 * Application entry class for the bundle integration test. It is laid down directly in the outer bundle (loaded via
 * the {@code jar:} path) and calls into {@link DepGreeter}, which lives only in a nested library jar (loaded via the
 * {@code singularity:} path). When launched it records its forwarded arguments and the dependency's greeting to the
 * file named by {@code args[0]} so the launching test can confirm both class-loading paths fired.
 */
public class EntryPointApp {

  public static void main (String[] args)
    throws Exception {

    StringBuilder builder = new StringBuilder();

    for (int index = 1; index < args.length; index++) {
      if (index > 1) {
        builder.append(' ');
      }
      builder.append(args[index]);
    }

    builder.append('|').append(DepGreeter.greet());

    Files.writeString(Path.of(args[0]), builder.toString(), StandardCharsets.UTF_8);
  }
}
