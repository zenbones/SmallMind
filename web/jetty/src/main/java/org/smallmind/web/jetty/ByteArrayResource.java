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
package org.smallmind.web.jetty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Jetty {@link Resource} implementation backed by an in-memory byte array.
 * This is primarily used to feed keystore and truststore content to Jetty
 * without touching the filesystem.
 */
public class ByteArrayResource extends Resource {

  private final byte[] bytes;

  /**
   * Constructs a resource that reads from the supplied byte buffer.
   *
   * @param bytes the bytes that will be exposed by this resource
   */
  public ByteArrayResource (byte[] bytes) {

    this.bytes = bytes;
  }

  /**
   * Always indicates that the byte content can be read.
   *
   * @return {@code true} because the underlying byte array is always readable
   */
  @Override
  public boolean isReadable () {

    return true;
  }

  /**
   * This resource is not considered contained in another resource because it is memory-only.
   *
   * @param resource ignored container candidate
   * @return {@code false} because containment is not supported
   */
  @Override
  public boolean isContainedIn (Resource resource) {

    return false;
  }

  /**
   * Always reports that the resource exists because the byte array is present.
   *
   * @return {@code true} to indicate availability
   */
  @Override
  public boolean exists () {

    return true;
  }

  /**
   * Indicates that this resource is not a directory.
   *
   * @return {@code false} because the resource is byte content, not a directory
   */
  @Override
  public boolean isDirectory () {

    return false;
  }

  /**
   * This resource has no filesystem representation.
   *
   * @return {@code null} because there is no path to expose
   */
  @Override
  public Path getPath () {

    return null;
  }

  /**
   * This resource has no concrete URI because it is held in memory.
   *
   * @return {@code null} to denote a missing URI
   */
  @Override
  public URI getURI () {

    return null;
  }

  /**
   * Returns no name because the resource is anonymous byte content.
   *
   * @return {@code null} as no stable name is available
   */
  @Override
  public String getName () {

    return null;
  }

  /**
   * Returns no filename because the resource is not backed by a file.
   *
   * @return {@code null} since no filename is applicable
   */
  @Override
  public String getFileName () {

    return null;
  }

  /**
   * Resolution of sub-resources is not supported for in-memory content.
   *
   * @param subUriPath the sub-path being requested
   * @return {@code null} because hierarchical lookup is not available
   */
  @Override
  public Resource resolve (String subUriPath) {

    return null;
  }

  /**
   * Provides the size of the underlying byte buffer.
   *
   * @return the length of the supplied byte array
   */
  @Override
  public long length () {

    return bytes.length;
  }

  /**
   * Opens a stream for reading the stored bytes.
   *
   * @return an input stream positioned at the beginning of the buffer
   */
  @Override
  public InputStream newInputStream () {

    return new ByteArrayInputStream(bytes);
  }
}
