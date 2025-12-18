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
package org.smallmind.web.jetty.installer;

// @ServicePath("/<service Uri>")

/**
 * Captures metadata needed to publish a SOAP web service through Jetty's JAX-WS support.
 */
public class WebServiceInstaller {

  private String path;
  private Object service;
  private Boolean asyncSupported;

  /**
   * Creates an installer for a SOAP service.
   *
   * @param path    relative URL path where the service is exposed
   * @param service service implementation instance
   */
  public WebServiceInstaller (String path, Object service) {

    this.path = path;
    this.service = service;
  }

  /**
   * Retrieves the relative URL path for the service.
   *
   * @return service path
   */
  public String getPath () {

    return path;
  }

  /**
   * Sets the relative URL path for the service.
   *
   * @param path service path to publish
   */
  public void setPath (String path) {

    this.path = path;
  }

  /**
   * Returns the service implementation instance.
   *
   * @return the SOAP service to publish
   */
  public Object getService () {

    return service;
  }

  /**
   * Assigns the service implementation to publish.
   *
   * @param service the SOAP service instance
   */
  public void setService (Object service) {

    this.service = service;
  }

  /**
   * Indicates whether the service supports asynchronous processing.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} if unspecified
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Configures whether the service supports asynchronous invocation.
   *
   * @param asyncSupported {@code true} to enable async support, {@code false} to disable, {@code null} to leave unset
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
