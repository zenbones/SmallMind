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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import freemarker.cache.TemplateLoader;

/**
 * FreeMarker {@link TemplateLoader} backed by the local file system, with an optional base path that template names are resolved against.
 */
public class FileSystemTemplateLoader implements TemplateLoader {

  private Path basePath;

  /**
   * Creates a loader without a fixed base path; template names are used as absolute file-system paths.
   */
  public FileSystemTemplateLoader () {

  }

  /**
   * Creates a loader that resolves all template names relative to the given base directory.
   *
   * @param basePath root directory used to resolve template names
   */
  public FileSystemTemplateLoader (Path basePath) {

    this.basePath = basePath;
  }

  /**
   * Sets the base directory against which relative template names are resolved.
   *
   * @param basePath root directory for template resolution; {@code null} to use absolute names
   */
  public void setBasePath (Path basePath) {

    this.basePath = basePath;
  }

  /**
   * Returns a {@link FileSystemTemplateSource} for the given name, resolving it against the configured base path or treating it as an absolute path when no base is set.
   *
   * @param name the template name or path as provided by FreeMarker
   * @return a {@link FileSystemTemplateSource} representing the resolved file; never {@code null}
   */
  @Override
  public Object findTemplateSource (String name) {

    if (basePath != null) {

      // Resolve relative to the configured base directory.
      return new FileSystemTemplateSource(basePath.resolve(name));
    } else {

      Path filePath;

      // Adding a slash back in because freemarker removes starting slashes as a security measure
      return new FileSystemTemplateSource(((filePath = Paths.get(name)).isAbsolute()) ? filePath : Paths.get("/" + name));
    }
  }

  /**
   * Returns the last-modified timestamp of the template file in milliseconds, or {@code -1} if the timestamp cannot be determined.
   *
   * @param templateSource a {@link FileSystemTemplateSource} previously returned by {@link #findTemplateSource}
   * @return last-modified time in milliseconds, or {@code -1} if unavailable
   */
  @Override
  public long getLastModified (Object templateSource) {

    try {

      return ((FileSystemTemplateSource)templateSource).getLastModified();
    } catch (IOException ioException) {

      // FreeMarker treats -1 as unknown; swallow I/O errors in timestamp lookup.
      return -1;
    }
  }

  /**
   * Opens and returns a character reader for the template file using the specified encoding.
   *
   * @param templateSource a {@link FileSystemTemplateSource} previously returned by {@link #findTemplateSource}
   * @param encoding       the character encoding to apply when reading the file
   * @return a reader positioned at the start of the template file
   * @throws IOException if the file cannot be opened or the encoding is unsupported
   */
  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    return new InputStreamReader(((FileSystemTemplateSource)templateSource).getInputStream(), encoding);
  }

  /**
   * Closes the input stream held by the given template source.
   *
   * @param templateSource a {@link FileSystemTemplateSource} previously returned by {@link #findTemplateSource}
   * @throws IOException if closing the stream fails
   */
  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((FileSystemTemplateSource)templateSource).close();
  }
}
