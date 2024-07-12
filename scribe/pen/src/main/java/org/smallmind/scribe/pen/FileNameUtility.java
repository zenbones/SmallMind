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
package org.smallmind.scribe.pen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.smallmind.nutsnbolts.io.PathUtility;

public class FileNameUtility {

  public static Path calculateUniquePath (Path desiredPath, char separator, String timestampSuffix, boolean alwaysUseIndex) {

    Path calculatedPath;
    Path parentPath = desiredPath.getParent();
    StringBuilder baseNameBuilder;
    StringBuilder uniqueNameBuilder;
    String fileName;
    boolean first = true;
    int dotPos;
    int uniqueCount = 0;

    baseNameBuilder = new StringBuilder();
    fileName = PathUtility.fileNameAsString(desiredPath);

    if ((dotPos = fileName.lastIndexOf('.')) >= 0) {
      baseNameBuilder.append(fileName, 0, dotPos);
    } else {
      baseNameBuilder.append(fileName);
    }

    baseNameBuilder.append(separator);
    baseNameBuilder.append(timestampSuffix);

    do {
      uniqueNameBuilder = new StringBuilder(baseNameBuilder);

      if (alwaysUseIndex || (!first)) {
        uniqueNameBuilder.append(separator);
        uniqueNameBuilder.append(uniqueCount++);
      }

      if (dotPos >= 0) {
        uniqueNameBuilder.append(fileName.substring(dotPos));
      }

      first = false;
    } while (Files.exists(calculatedPath = (parentPath == null) ? Paths.get(uniqueNameBuilder.toString()) : parentPath.resolve(uniqueNameBuilder.toString())));

    return calculatedPath;
  }
}
