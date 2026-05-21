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
 * Value object describing a host-to-container volume bind mount. The host path is resolved
 * from the thread context classloader at construction time, with automatic Windows path
 * normalization applied. Throws {@link MissingHostResourceException} immediately if the
 * classpath resource cannot be located, catching configuration errors early.
 *
 * <p>Instances are created exclusively via the {@link #create(String, String, AccessMode)}
 * factory method.
 */
public class DockerVolume {

  private final String hostPath;
  private final String localPath;
  private final AccessMode accessMode;

  /**
   * Private constructor — use {@link #create(String, String, AccessMode)} instead.
   *
   * @param hostPath   the resolved absolute host filesystem path
   * @param localPath  the absolute container-side mount path
   * @param accessMode the volume access mode ({@code ro} or {@code rw})
   */
  private DockerVolume (String hostPath, String localPath, AccessMode accessMode) {

    this.hostPath = hostPath;
    this.localPath = localPath;
    this.accessMode = accessMode;
  }

  /**
   * Creates a new {@link DockerVolume} by resolving {@code hostResource} from the thread
   * context classloader. The resulting URL is converted to an absolute OS path, with Windows
   * paths normalized from the URL form to a drive-letter-prefixed backslash path.
   *
   * @param hostResource the classpath-relative resource name to bind onto the container
   * @param localPath    the absolute path inside the container where the volume is mounted
   * @param accessMode   the volume access mode ({@code ro} or {@code rw})
   * @return a new {@link DockerVolume} instance; never {@code null}
   * @throws MissingHostResourceException if the classpath resource cannot be found by the
   *                                      thread context classloader
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
   * Returns the resolved absolute host filesystem path to bind into the container.
   *
   * @return the host path; never {@code null}
   */
  public String getHostPath () {

    return hostPath;
  }

  /**
   * Returns the absolute path inside the container where the host volume is mounted.
   *
   * @return the container-side mount path; never {@code null}
   */
  public String getLocalPath () {

    return localPath;
  }

  /**
   * Returns the access mode governing whether the container may write to this volume.
   *
   * @return {@code AccessMode.ro} for read-only or {@code AccessMode.rw} for read-write
   */
  public AccessMode getAccessMode () {

    return accessMode;
  }
}
