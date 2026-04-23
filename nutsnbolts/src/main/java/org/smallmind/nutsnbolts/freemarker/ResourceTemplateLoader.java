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
import freemarker.cache.TemplateLoader;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeResourceGenerator;

/**
 * FreeMarker {@link TemplateLoader} that resolves template names through the {@link ResourceParser} abstraction, supporting URLs, classpath references, and any other resource scheme recognized by {@link ResourceTypeResourceGenerator}.
 */
public class ResourceTemplateLoader implements TemplateLoader {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeResourceGenerator());

  /**
   * Parses the given name as a resource identifier and returns a {@link ResourceTemplateSource} wrapping the resulting {@link org.smallmind.nutsnbolts.resource.Resource}.
   *
   * @param name the template resource identifier, interpreted by {@link ResourceParser}
   * @return a {@link ResourceTemplateSource} for the resolved resource
   * @throws IOException if the name cannot be parsed or the resource cannot be located
   */
  @Override
  public Object findTemplateSource (String name)
    throws IOException {

    try {
      // Leverage ResourceParser so template names can be URLs, classpath refs, etc.
      return new ResourceTemplateSource(RESOURCE_PARSER.parseResource(name));
    } catch (ResourceException resourceException) {
      throw new IOException(resourceException);
    }
  }

  /**
   * Returns {@code -1} because the resource abstraction does not expose last-modified timestamps.
   *
   * @param templateSource ignored
   * @return {@code -1} always
   */
  @Override
  public long getLastModified (Object templateSource) {

    return -1;
  }

  /**
   * Opens and returns a character reader for the resource template using the specified encoding.
   *
   * @param templateSource a {@link ResourceTemplateSource} previously returned by {@link #findTemplateSource}
   * @param encoding       the character encoding to apply when reading the resource
   * @return a reader over the template content
   * @throws IOException if the resource cannot be opened or the encoding is unsupported
   */
  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    try {
      return new InputStreamReader(((ResourceTemplateSource)templateSource).getInputStream(), encoding);
    } catch (ResourceException resourceException) {
      throw new IOException(resourceException);
    }
  }

  /**
   * Closes the input stream held by the given template source, if it was opened.
   *
   * @param templateSource a {@link ResourceTemplateSource} previously returned by {@link #findTemplateSource}
   * @throws IOException if closing the stream fails
   */
  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((ResourceTemplateSource)templateSource).close();
  }
}
