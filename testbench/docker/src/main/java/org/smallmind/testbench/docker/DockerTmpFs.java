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
 * Immutable description of a tmpfs (in-memory) mount for a Docker container, used to give I/O-heavy
 * services a fast, ephemeral filesystem during testing — for instance the MySQL data directory in
 * {@link DockerApplication#MYSQL}.
 *
 * <p>Construct instances through {@link #create(String, String)}; the constructor is private.
 */
public class DockerTmpFs {

  private final String path;
  private final String parameters;

  /**
   * Stores the tmpfs fields. Use {@link #create(String, String)} rather than calling this directly.
   *
   * @param path the absolute container path mounted as tmpfs
   * @param parameters the tmpfs mount options passed to the Docker daemon
   */
  private DockerTmpFs (String path, String parameters) {

    this.path = path;
    this.parameters = parameters;
  }

  /**
   * Creates a tmpfs descriptor for the given container path and mount options.
   *
   * @param path the absolute container path to mount as tmpfs, such as {@code "/var/lib/mysql"}
   * @param parameters the tmpfs mount options accepted by the Docker daemon, such as
   * {@code "rw,noexec,nosuid,size=1024m"}
   * @return a new tmpfs descriptor; never {@code null}
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
   * Returns the tmpfs mount options passed to the Docker daemon.
   *
   * @return the mount options, such as {@code "rw,noexec,nosuid,size=1024m"}; never {@code null}
   */
  public String getParameters () {

    return parameters;
  }
}
