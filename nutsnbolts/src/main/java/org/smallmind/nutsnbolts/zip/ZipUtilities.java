/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.smallmind.nutsnbolts.io.FileIterator;

public class ZipUtilities {

  public static void compressDirectory (File directory, OutputStream outputStream)
    throws IOException {

    ZipOutputStream zipOutputStream;
    byte[] buffer = new byte[8192];

    zipOutputStream = new ZipOutputStream(outputStream);
    for (File file : new FileIterator(directory)) {

      FileInputStream fileInputStream;
      int bytesRead;

      fileInputStream = new FileInputStream(file);
      zipOutputStream.putNextEntry(new ZipEntry(file.getCanonicalPath()));

      while ((bytesRead = fileInputStream.read(buffer)) > 0) {
        zipOutputStream.write(buffer, 0, bytesRead);
      }

      zipOutputStream.closeEntry();
      fileInputStream.close();
    }

    zipOutputStream.close();
  }
}
