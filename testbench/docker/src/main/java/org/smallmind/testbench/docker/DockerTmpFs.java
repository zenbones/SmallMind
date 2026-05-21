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

/**
 * Value object describing a tmpfs mount to be applied to a Docker container.
 * Used to create fast, ephemeral in-memory filesystems for applications that perform
 * heavy disk I/O during testing (e.g., the MySQL data directory).
 *
 * <p>Instances are created exclusively via the {@link #create(String, String)} factory method.
 */
public class DockerTmpFs {

  private final String path;
  private final String parameters;

  /**
   * Private constructor — use {@link #create(String, String)} instead.
   *
   * @param path       the absolute container path to mount as tmpfs
   * @param parameters the mount option string passed to the Docker daemon
   */
  private DockerTmpFs (String path, String parameters) {

    this.path = path;
    this.parameters = parameters;
  }

  /**
   * Creates a new {@link DockerTmpFs} descriptor with the given path and mount parameters.
   *
   * @param path       the absolute container path to mount as tmpfs (e.g., {@code "/var/lib/mysql"})
   * @param parameters mount options accepted by the Docker daemon (e.g., {@code "rw,noexec,nosuid,size=1024m"})
   * @return a new {@link DockerTmpFs} instance; never {@code null}
   */
  public static DockerTmpFs create (String path, String parameters) {

    return new DockerTmpFs(path, parameters);
  }

  /**
   * Returns the absolute container path at which the tmpfs is mounted.
   *
   * @return the mount path; never {@code null}
   */
  public String getPath () {

    return path;
  }

  /**
   * Returns the mount option string passed to the Docker daemon when creating the tmpfs.
   *
   * @return the mount parameters (e.g., {@code "rw,noexec,nosuid,size=1024m"}); never {@code null}
   */
  public String getParameters () {

    return parameters;
  }
}
