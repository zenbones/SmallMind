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
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;

/**
 * Handle for a FreeMarker template backed by the {@link Resource} abstraction, lazily opening the resource stream on first request and used by {@link ResourceTemplateLoader}.
 */
public class ResourceTemplateSource {

  private final Resource resource;
  private InputStream inputStream;

  /**
   * Constructs a source backed by the given resource; the stream is not opened until {@link #getInputStream} is first called.
   *
   * @param resource the resource that supplies the template content
   */
  public ResourceTemplateSource (Resource resource) {

    this.resource = resource;
  }

  /**
   * Returns the underlying resource that supplies the template content.
   *
   * @return the wrapped {@link Resource}
   */
  public Resource getResource () {

    return resource;
  }

  /**
   * Returns an input stream for reading the template, opening it on first call and returning the same stream on subsequent calls.
   *
   * @return input stream ready for reading the template content
   * @throws ResourceException if the resource's stream cannot be opened
   */
  public synchronized InputStream getInputStream ()
    throws ResourceException {

    if (inputStream == null) {
      inputStream = resource.getInputStream();
    }

    return inputStream;
  }

  /**
   * Closes the resource input stream if it was previously opened.
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
   * Returns a hash code derived from the underlying resource.
   *
   * @return hash code of the wrapped resource
   */
  @Override
  public int hashCode () {

    return resource.hashCode();
  }

  /**
   * Returns {@code true} when the other object is a {@link ResourceTemplateSource} wrapping an equal resource.
   *
   * @param obj the object to compare with this source
   * @return {@code true} if both sources wrap equal resources
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ResourceTemplateSource) && ((ResourceTemplateSource)obj).getResource().equals(resource);
  }
}
