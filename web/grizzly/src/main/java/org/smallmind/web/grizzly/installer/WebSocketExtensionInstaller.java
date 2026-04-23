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
package org.smallmind.web.grizzly.installer;

import jakarta.websocket.Extension;

/**
 * Descriptor for one or more WebSocket extensions to merge into a specific endpoint's configuration.
 */
public class WebSocketExtensionInstaller extends GrizzlyInstaller {

  private Extension[] extensions;
  private Class<?> endpointClass;
  private String path;

  /**
   * Returns the installer type discriminator.
   *
   * @return {@link GrizzlyInstallerType#WEB_SOCKET_EXTENSION}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.WEB_SOCKET_EXTENSION;
  }

  /**
   * Returns the extensions to add to the target endpoint's configuration.
   *
   * @return array of WebSocket extensions
   */
  public Extension[] getExtensions () {

    return extensions;
  }

  /**
   * Sets the extensions to add to the target endpoint's configuration.
   *
   * @param extensions WebSocket extensions to register
   */
  public void setExtensions (Extension[] extensions) {

    this.extensions = extensions;
  }

  /**
   * Returns the endpoint class that these extensions target.
   *
   * @return WebSocket endpoint class
   */
  public Class<?> getEndpointClass () {

    return endpointClass;
  }

  /**
   * Sets the endpoint class that these extensions target.
   *
   * @param endpointClass WebSocket endpoint class
   */
  public void setEndpointClass (Class<?> endpointClass) {

    this.endpointClass = endpointClass;
  }

  /**
   * Returns the endpoint path used to match the target endpoint during merging.
   *
   * @return endpoint path
   */
  public String getPath () {

    return path;
  }

  /**
   * Sets the endpoint path used to match the target endpoint during merging.
   *
   * @param path endpoint path
   */
  public void setPath (String path) {

    this.path = path;
  }
}
