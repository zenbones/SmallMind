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
package org.smallmind.testbench.docker;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import com.github.dockerjava.api.model.AccessMode;

/**
 * Immutable description of a host-to-container volume bind whose host side is a classpath resource.
 * The resource is resolved against the thread context classloader at construction, and a resolution
 * failure raises {@link MissingHostResourceException} right away, so a misconfigured fixture fails
 * when it is declared rather than when the container is started.
 *
 * <p>Construct instances through {@link #create(String, String, AccessMode)}; the constructor is
 * private.
 */
public class DockerVolume {

  private final String hostPath;
  private final String localPath;
  private final AccessMode accessMode;

  /**
   * Stores the already-resolved volume fields. Use {@link #create(String, String, AccessMode)}
   * rather than calling this directly.
   *
   * @param hostPath the resolved absolute host path
   * @param localPath the absolute container-side mount path
   * @param accessMode the volume access mode ({@code ro} or {@code rw})
   */
  private DockerVolume (String hostPath, String localPath, AccessMode accessMode) {

    this.hostPath = hostPath;
    this.localPath = localPath;
    this.accessMode = accessMode;
  }

  /**
   * Creates a volume bind whose host side is resolved from a classpath resource. The resource is
   * located via the thread context classloader and its URL converted to an absolute filesystem path.
   *
   * @param hostResource the classpath-relative resource name providing the host side of the bind
   * @param localPath the absolute container path the volume is mounted at
   * @param accessMode the volume access mode ({@code ro} or {@code rw})
   * @return a new volume descriptor; never {@code null}
   * @throws MissingHostResourceException if {@code hostResource} cannot be found on the classpath or
   * its URL is malformed
   */
  public static DockerVolume create (String hostResource, String localPath, AccessMode accessMode) {

    URL hostResourceURL;

    if ((hostResourceURL = Thread.currentThread().getContextClassLoader().getResource(hostResource)) == null) {
      throw new MissingHostResourceException(hostResource);
    } else {
      try {

        return new DockerVolume(Paths.get(hostResourceURL.toURI()).toString(), localPath, accessMode);
      } catch (URISyntaxException uriSyntaxException) {
        throw new MissingHostResourceException(hostResource);
      }
    }
  }

  /**
   * Returns the resolved absolute host path bound into the container.
   *
   * @return the host path; never {@code null}
   */
  public String getHostPath () {

    return hostPath;
  }

  /**
   * Returns the absolute container path at which the host volume is mounted.
   *
   * @return the container-side mount path; never {@code null}
   */
  public String getLocalPath () {

    return localPath;
  }

  /**
   * Returns the access mode governing whether the container may write to the volume.
   *
   * @return {@link AccessMode#ro} for read-only or {@link AccessMode#rw} for read-write
   */
  public AccessMode getAccessMode () {

    return accessMode;
  }
}
