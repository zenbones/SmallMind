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
 * Handle for a file-system-based FreeMarker template, lazily opening the underlying {@link InputStream} on first request and used by {@link FileSystemTemplateLoader}.
 */
public class FileSystemTemplateSource {

  private final Path templatePath;
  private InputStream inputStream;

  /**
   * Constructs a source backed by the specified file-system path; the stream is not opened until {@link #getInputStream} is first called.
   *
   * @param templatePath path to the template file
   */
  public FileSystemTemplateSource (Path templatePath) {

    this.templatePath = templatePath;
  }

  /**
   * Returns the file-system path of the template file.
   *
   * @return the template's {@link Path}
   */
  public Path getTemplatePath () {

    return templatePath;
  }

  /**
   * Returns the last-modified time of the template file in milliseconds since the epoch.
   *
   * @return last-modified timestamp in milliseconds
   * @throws IOException if the file's metadata cannot be read
   */
  public long getLastModified ()
    throws IOException {

    return Files.getLastModifiedTime(templatePath).toMillis();
  }

  /**
   * Returns {@code true} if the path refers to an existing regular file.
   *
   * @return {@code true} if the template file exists
   */
  public boolean exists () {

    return Files.isRegularFile(templatePath);
  }

  /**
   * Returns an input stream for reading the template, opening it on first call and returning the same stream on subsequent calls.
   *
   * @return input stream positioned at the start of the template file
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
   * Closes the input stream if it was previously opened.
   *
   * @throws IOException if closing the stream fails
   */
  public synchronized void close ()
    throws IOException {

    if (inputStream != null) {
      inputStream.close();
    }
  }

  /**
   * Returns a hash code derived from the underlying file path.
   *
   * @return hash code of the template path
   */
  @Override
  public int hashCode () {

    return templatePath.hashCode();
  }

  /**
   * Returns {@code true} when the other object is a {@link FileSystemTemplateSource} pointing to the same file-system path.
   *
   * @param obj the object to compare with this source
   * @return {@code true} if both sources refer to the same path
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof FileSystemTemplateSource) && ((FileSystemTemplateSource)obj).getTemplatePath().equals(templatePath);
  }
}
