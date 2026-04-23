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
 * FreeMarker {@link TemplateLoader} that resolves templates from the classpath, supporting both absolute resource names and names resolved relative to an anchor class's package.
 */
public class ClassPathTemplateLoader implements TemplateLoader {

  private final ClassLoader classLoader;
  private final boolean relative;
  private Class<?> anchorClass;

  /**
   * Creates a loader that resolves absolute classpath resource names using the current thread's context class loader.
   */
  public ClassPathTemplateLoader () {

    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a loader that resolves absolute classpath resource names using the supplied class loader.
   *
   * @param classLoader class loader to use when locating template resources
   */
  public ClassPathTemplateLoader (ClassLoader classLoader) {

    this.classLoader = classLoader;

    relative = false;
  }

  /**
   * Creates a loader that resolves template names relative to the package of the given anchor class using that class's own class loader.
   *
   * @param anchorClass class whose package path is used as the base for relative template names
   */
  public ClassPathTemplateLoader (Class<?> anchorClass) {

    this(anchorClass, false);
  }

  /**
   * Creates a loader anchored to the given class; when {@code relative} is {@code true}, template names are prefixed with the anchor class's package path before lookup.
   *
   * @param anchorClass class whose package path is used as the base for template names
   * @param relative    {@code true} to prepend the anchor class's package path to all template names
   */
  public ClassPathTemplateLoader (Class<?> anchorClass, boolean relative) {

    this.anchorClass = anchorClass;
    this.relative = relative;

    classLoader = anchorClass.getClassLoader();
  }

  /**
   * Returns the anchor class whose package is used as the base path for relative template resolution.
   *
   * @return the anchor class, or {@code null} if this loader uses absolute names
   */
  public Class<?> getAnchorClass () {

    return anchorClass;
  }

  /**
   * Returns the class loader used to locate classpath template resources.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }

  /**
   * Locates a template by name on the classpath, optionally prefixing the name with the anchor class's package path, and returns a {@link ClassPathTemplateSource} or {@code null} if the resource does not exist.
   *
   * @param name the template resource name or path
   * @return a {@link ClassPathTemplateSource} if the resource exists, or {@code null} otherwise
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
   * Returns {@code -1} because classpath resources do not expose last-modified timestamps.
   *
   * @param templateSource ignored
   * @return {@code -1} always
   */
  @Override
  public long getLastModified (Object templateSource) {

    // Classpath resources generally don't expose last-modified timestamps.
    return -1;
  }

  /**
   * Opens and returns a character reader for the template resource using the specified encoding.
   *
   * @param templateSource a {@link ClassPathTemplateSource} previously returned by {@link #findTemplateSource}
   * @param encoding       the character encoding to apply to the input stream
   * @return a reader over the template content
   * @throws IOException if the underlying input stream cannot be opened or the encoding is unsupported
   */
  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    return new InputStreamReader(((ClassPathTemplateSource)templateSource).getInputStream(), encoding);
  }

  /**
   * Closes the input stream held by the given template source.
   *
   * @param templateSource a {@link ClassPathTemplateSource} previously returned by {@link #findTemplateSource}
   * @throws IOException if closing the stream fails
   */
  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((ClassPathTemplateSource)templateSource).close();
  }
}
