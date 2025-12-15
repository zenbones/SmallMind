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
 * Wraps a {@link Resource} for use as a FreeMarker template source.
 */
public class ResourceTemplateSource {

  private final Resource resource;
  private InputStream inputStream;

  /**
   * @param resource resource supplying template content
   */
  public ResourceTemplateSource (Resource resource) {

    this.resource = resource;
  }

  /**
   * @return underlying resource
   */
  public Resource getResource () {

    return resource;
  }

  /**
   * Lazily obtains an input stream from the underlying resource.
   *
   * @return input stream ready for reading
   * @throws ResourceException if the stream cannot be opened
   */
  public synchronized InputStream getInputStream ()
    throws ResourceException {

    if (inputStream == null) {
      inputStream = resource.getInputStream();
    }

    return inputStream;
  }

  /**
   * Closes the resource stream if it was opened.
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
   * Computes a hash based on the wrapped resource.
   *
   * @return hash value for collections
   */
  @Override
  public int hashCode () {

    return resource.hashCode();
  }

  /**
   * Compares sources by their underlying resource.
   *
   * @param obj object to compare
   * @return {@code true} when resources match
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ResourceTemplateSource) && ((ResourceTemplateSource)obj).getResource().equals(resource);
  }
}
