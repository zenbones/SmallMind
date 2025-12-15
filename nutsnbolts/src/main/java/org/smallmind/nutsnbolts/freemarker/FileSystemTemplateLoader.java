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
 * FreeMarker {@link TemplateLoader} backed by the local file system.
 * Optionally anchors lookups at a base path to prevent template path traversal.
 */
public class FileSystemTemplateLoader implements TemplateLoader {

  private Path basePath;

  /**
   * Creates a loader without a base path; callers should supply absolute template names.
   */
  public FileSystemTemplateLoader () {

  }

  /**
   * Creates a loader anchored at the specified base path.
   *
   * @param basePath root directory from which templates will be resolved
   */
  public FileSystemTemplateLoader (Path basePath) {

    this.basePath = basePath;
  }

  /**
   * Sets or replaces the base path used to resolve template names.
   *
   * @param basePath root directory for templates
   */
  public void setBasePath (Path basePath) {

    this.basePath = basePath;
  }

  /**
   * Resolves the named template against the base path (if configured) or the raw file system.
   *
   * @param name template name/path
   * @return template source reference
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
   * Reports the last-modified timestamp for the template source.
   *
   * @param templateSource file-system template source
   * @return timestamp in milliseconds or {@code -1} if unavailable
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
   * Opens a reader for the template using the requested encoding.
   *
   * @param templateSource file-system template source
   * @param encoding       character encoding to apply
   * @return reader over the template content
   * @throws IOException if the file cannot be opened
   */
  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    return new InputStreamReader(((FileSystemTemplateSource)templateSource).getInputStream(), encoding);
  }

  /**
   * Closes the underlying template stream.
   *
   * @param templateSource template source to close
   * @throws IOException if closing fails
   */
  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((FileSystemTemplateSource)templateSource).close();
  }
}
