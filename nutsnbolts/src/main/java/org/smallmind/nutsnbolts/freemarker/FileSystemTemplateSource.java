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
package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a file-system-based FreeMarker template source, deferring stream creation until requested.
 */
public class FileSystemTemplateSource {

  private final Path templatePath;
  private InputStream inputStream;

  /**
   * @param templatePath path to the template file
   */
  public FileSystemTemplateSource (Path templatePath) {

    this.templatePath = templatePath;
  }

  /**
   * @return path to the underlying template file
   */
  public Path getTemplatePath () {

    return templatePath;
  }

  /**
   * @return last-modified timestamp in milliseconds
   * @throws IOException if file metadata cannot be read
   */
  public long getLastModified ()
    throws IOException {

    return Files.getLastModifiedTime(templatePath).toMillis();
  }

  /**
   * @return {@code true} if the path points to a regular file
   */
  public boolean exists () {

    return Files.isRegularFile(templatePath);
  }

  /**
   * Opens or returns a cached input stream to the template.
   *
   * @return input stream positioned at the start of the file
   * @throws IOException if the file cannot be opened
   */
  public synchronized InputStream getInputStream ()
    throws IOException {

    if (inputStream == null) {
      inputStream = Files.newInputStream(templatePath);
    }

    return inputStream;
  }

  /**
   * Closes the input stream if it was opened.
   *
   * @throws IOException if closure fails
   */
  public synchronized void close ()
    throws IOException {

    if (inputStream != null) {
      inputStream.close();
    }
  }

  /**
   * Computes a hash based on the underlying file path.
   *
   * @return hash value for collections
   */
  @Override
  public int hashCode () {

    return templatePath.hashCode();
  }

  /**
   * Compares sources by underlying path.
   *
   * @param obj object to compare
   * @return {@code true} when the template paths match
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof FileSystemTemplateSource) && ((FileSystemTemplateSource)obj).getTemplatePath().equals(templatePath);
  }
}
