/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.spark.tanukisoft.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.smallmind.nutsnbolts.io.FileIterator;

public enum CompressionType {

  JAR("jar") {
    @Override
    public void compress (File compressedFile, File directoryToCompress)
      throws IOException {

      FileOutputStream fileOutputStream;
      JarOutputStream jarOutputStream;
      JarEntry jarEntry;

      fileOutputStream = new FileOutputStream(compressedFile);
      jarOutputStream = new JarOutputStream(fileOutputStream, new Manifest());
      for (File outputFile : new FileIterator(directoryToCompress)) {
        if (!outputFile.equals(compressedFile)) {
          jarEntry = new JarEntry(outputFile.getCanonicalPath().substring(directoryToCompress.getAbsolutePath().length() + 1).replace(System.getProperty("file.separator"), "/"));
          jarEntry.setTime(outputFile.lastModified());
          jarOutputStream.putNextEntry(jarEntry);
          squeezeFileIntoJar(jarOutputStream, outputFile);
        }
      }
      jarOutputStream.close();
      fileOutputStream.close();
    }

    private void squeezeFileIntoJar (JarOutputStream jarOutputStream, File outputFile)
      throws IOException {

      FileInputStream inputStream;
      byte[] buffer = new byte[8192];
      int bytesRead;

      inputStream = new FileInputStream(outputFile);
      while ((bytesRead = inputStream.read(buffer)) >= 0) {
        jarOutputStream.write(buffer, 0, bytesRead);
      }
      inputStream.close();
    }
  },
  ZIP("zip") {
    @Override
    public void compress (File compressedFile, File directoryToCompress)
      throws IOException {

      FileOutputStream fileOutputStream;
      ZipOutputStream zipOutputStream;
      ZipEntry zipEntry;

      fileOutputStream = new FileOutputStream(compressedFile);
      zipOutputStream = new ZipOutputStream(fileOutputStream);
      for (File outputFile : new FileIterator(directoryToCompress)) {
        if (!outputFile.equals(compressedFile)) {
          zipEntry = new ZipEntry(outputFile.getCanonicalPath().substring(directoryToCompress.getAbsolutePath().length() + 1).replace(System.getProperty("file.separator"), "/"));
          zipEntry.setTime(outputFile.lastModified());
          zipOutputStream.putNextEntry(zipEntry);
          squeezeFileIntoZip(zipOutputStream, outputFile);
        }
      }
      zipOutputStream.close();
      fileOutputStream.close();
    }

    private void squeezeFileIntoZip (ZipOutputStream zipOutputStream, File outputFile)
      throws IOException {

      FileInputStream inputStream;
      byte[] buffer = new byte[8192];
      int bytesRead;

      inputStream = new FileInputStream(outputFile);
      while ((bytesRead = inputStream.read(buffer)) >= 0) {
        zipOutputStream.write(buffer, 0, bytesRead);
      }
      inputStream.close();
    }
  };
  private String extension;

  private CompressionType (String extension) {

    this.extension = extension;
  }

  public abstract void compress (File compressedFile, File directoryToCompress)
    throws IOException;

  public String getExtension () {

    return extension;
  }
}
