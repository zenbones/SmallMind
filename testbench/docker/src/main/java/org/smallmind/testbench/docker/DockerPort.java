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
 * A single Docker port mapping, pairing a container-internal service port with an optional external
 * host port. The service port is fixed at construction; the host port is an optional override that
 * may be set afterward. When no override is set, the service port number is used on the host side as
 * well.
 */
public class DockerPort {

  private final int servicePort;
  private Integer externalPort;

  /**
   * Creates a mapping for the given service port with no host-port override, so the same number is
   * used on both sides until {@link #setExternalPort(Integer)} is called.
   *
   * @param servicePort the container-internal TCP port
   */
  public DockerPort (int servicePort) {

    this.servicePort = servicePort;
  }

  /**
   * Returns the container-internal service port.
   *
   * @return the service port
   */
  public int getServicePort () {

    return servicePort;
  }

  /**
   * Returns the host-side port override, or {@code null} when none is set; in that case the
   * {@linkplain #getServicePort() service port} is used on the host.
   *
   * @return the external host port, or {@code null}
   */
  public Integer getExternalPort () {

    return externalPort;
  }

  /**
   * Sets the host-side port to bind the service port to.
   *
   * @param externalPort the external host port, or {@code null} to clear the override and fall back
   * to the service port
   */
  public void setExternalPort (Integer externalPort) {

    this.externalPort = externalPort;
  }
}
