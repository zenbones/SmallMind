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

/**
 * FreeMarker {@link TemplateLoader} that resolves templates from the classpath.
 * Supports loading relative to an anchor class/package or directly from a class loader.
 */
public class ClassPathTemplateLoader implements TemplateLoader {

  private final ClassLoader classLoader;
  private final boolean relative;
  private Class<?> anchorClass;

  /**
   * Creates a loader using the thread context class loader.
   */
  public ClassPathTemplateLoader () {

    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a loader using the supplied class loader and absolute template names.
   *
   * @param classLoader class loader used to resolve resources
   */
  public ClassPathTemplateLoader (ClassLoader classLoader) {

    this.classLoader = classLoader;

    relative = false;
  }

  /**
   * Creates a loader that resolves templates relative to the package of an anchor class.
   *
   * @param anchorClass class whose package provides the base path
   */
  public ClassPathTemplateLoader (Class<?> anchorClass) {

    this(anchorClass, false);
  }

  /**
   * Creates a loader that resolves templates relative to the package of an anchor class.
   *
   * @param anchorClass class whose package provides the base path
   * @param relative    if {@code true}, prepend the anchor package path to template names
   */
  public ClassPathTemplateLoader (Class<?> anchorClass, boolean relative) {

    this.anchorClass = anchorClass;
    this.relative = relative;

    classLoader = anchorClass.getClassLoader();
  }

  /**
   * @return class whose package is used for relative template resolution, or {@code null}
   */
  public Class<?> getAnchorClass () {

    return anchorClass;
  }

  /**
   * @return class loader used to locate classpath resources
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }

  /**
   * Locates a template resource by name, optionally resolving relative to the anchor package.
   *
   * @param name template name/path
   * @return template source handle or {@code null} if not found
   */
  @Override
  public Object findTemplateSource (String name) {

    ClassPathTemplateSource source;

    // Resolve relative to the anchor package when configured, otherwise treat as absolute classpath resource.
    if (relative && (anchorClass != null)) {

      StringBuilder pathBuilder = new StringBuilder(anchorClass.getPackage().getName().replace('.', '/'));

      pathBuilder.append('/').append(name);
      source = new ClassPathTemplateSource(classLoader, pathBuilder.toString());
    } else {
      source = new ClassPathTemplateSource(classLoader, name);
    }

    return (source.exists()) ? source : null;
  }

  /**
   * Classpath resources do not expose modification times; returns {@code -1}.
   *
   * @param templateSource ignored
   * @return {@code -1} to indicate unknown modification time
   */
  @Override
  public long getLastModified (Object templateSource) {

    // Classpath resources generally don't expose last-modified timestamps.
    return -1;
  }

  /**
   * Opens a reader for the supplied template source using the requested encoding.
   *
   * @param templateSource classpath template source
   * @param encoding       character encoding to apply
   * @return reader over the template content
   * @throws IOException if the stream cannot be opened
   */
  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    return new InputStreamReader(((ClassPathTemplateSource)templateSource).getInputStream(), encoding);
  }

  /**
   * Closes the underlying template source stream.
   *
   * @param templateSource template source to close
   * @throws IOException if closing fails
   */
  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((ClassPathTemplateSource)templateSource).close();
  }
}
