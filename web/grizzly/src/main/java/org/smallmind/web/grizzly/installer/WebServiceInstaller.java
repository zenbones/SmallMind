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

// @ServicePath(value="/<service Uri>", context="/context")

/**
 * Binds a SOAP service object to a URI path within a specific Grizzly web application context.
 */
public class WebServiceInstaller {

  private String path;
  private Object service;
  private Boolean asyncSupported;

  /**
   * Constructs a descriptor associating a service with a path.
   *
   * @param path    endpoint URI relative to the application's SOAP root
   * @param service service bean to expose
   */
  public WebServiceInstaller (String path, Object service) {

    this.path = path;
    this.service = service;
  }

  /**
   * Returns the endpoint URI relative to the SOAP root.
   *
   * @return service path
   */
  public String getPath () {

    return path;
  }

  /**
   * Sets the endpoint URI relative to the SOAP root.
   *
   * @param path service path fragment
   */
  public void setPath (String path) {

    this.path = path;
  }

  /**
   * Returns the service bean to expose.
   *
   * @return service instance
   */
  public Object getService () {

    return service;
  }

  /**
   * Sets the service bean to expose.
   *
   * @param service service instance
   */
  public void setService (Object service) {

    this.service = service;
  }

  /**
   * Returns whether the JAX-WS handler should be marked as async capable.
   *
   * @return {@code Boolean.TRUE} to enable async, or {@code null} to use the default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Sets whether the JAX-WS handler should be marked as async capable.
   *
   * @param asyncSupported async-support flag, or {@code null} to use the default
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
