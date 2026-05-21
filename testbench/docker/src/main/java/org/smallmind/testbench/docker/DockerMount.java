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
import com.github.dockerjava.api.model.MountType;

/**
 * Value object describing a Docker mount configuration (typically a bind mount). The host path
 * is resolved from the thread context classloader at construction time. Throws
 * {@link MissingHostResourceException} immediately if the classpath resource cannot be located,
 * catching configuration errors early.
 *
 * <p>Instances are created exclusively via the {@link #create(MountType, String, String, boolean)}
 * factory method.
 */
public class DockerMount {

  private final MountType mountType;
  private final String hostPath;
  private final String localPath;
  private final boolean readOnly;

  /**
   * Private constructor — use {@link #create(MountType, String, String, boolean)} instead.
   *
   * @param mountType the Docker mount type (e.g., {@code BIND})
   * @param hostPath  the resolved absolute host filesystem path
   * @param localPath the absolute container-side mount path
   * @param readOnly  {@code true} to mount the path read-only inside the container
   */
  private DockerMount (MountType mountType, String hostPath, String localPath, boolean readOnly) {

    this.mountType = mountType;
    this.hostPath = hostPath;
    this.localPath = localPath;
    this.readOnly = readOnly;
  }

  /**
   * Creates a new {@link DockerMount} by resolving {@code hostResource} from the thread
   * context classloader. The resulting URL is converted to an absolute OS path.
   *
   * @param mountType    the Docker mount type (e.g., {@link MountType#BIND})
   * @param hostResource the classpath-relative resource name to mount onto the container
   * @param localPath    the absolute path inside the container where the resource is mounted
   * @param readOnly     {@code true} to mount the path read-only inside the container
   * @return a new {@link DockerMount} instance; never {@code null}
   * @throws MissingHostResourceException if the classpath resource cannot be found by the
   *                                      thread context classloader
   */
  public static DockerMount create (MountType mountType, String hostResource, String localPath, boolean readOnly) {

    URL hostResourceURL;

    if ((hostResourceURL = Thread.currentThread().getContextClassLoader().getResource(hostResource)) == null) {
      throw new MissingHostResourceException(hostResource);
    } else {
      try {

        return new DockerMount(mountType, Paths.get(hostResourceURL.toURI()).toString(), localPath, readOnly);
      } catch (URISyntaxException uriSyntaxException) {
        throw new MissingHostResourceException(hostResource);
      }
    }
  }

  /**
   * Returns the Docker mount type for this mount configuration.
   *
   * @return the mount type; never {@code null}
   */
  public MountType getMountType () {

    return mountType;
  }

  /**
   * Returns the resolved absolute host filesystem path to mount into the container.
   *
   * @return the host path; never {@code null}
   */
  public String getHostPath () {

    return hostPath;
  }

  /**
   * Returns the absolute path inside the container where the host resource is mounted.
   *
   * @return the container-side mount path; never {@code null}
   */
  public String getLocalPath () {

    return localPath;
  }

  /**
   * Returns whether the container-side mount path is read-only.
   *
   * @return {@code true} if the mount is read-only; {@code false} if it is read-write
   */
  public boolean isReadOnly () {

    return readOnly;
  }
}
