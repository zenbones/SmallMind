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
 * Simple descriptor that binds a SOAP service bean to a URI within a specific Grizzly context.
 */
public class WebServiceInstaller {

  private String path;
  private Object service;
  private Boolean asyncSupported;

  /**
   * @param path    service URI relative to the SOAP root
   * @param service service instance to expose
   */
  public WebServiceInstaller (String path, Object service) {

    this.path = path;
    this.service = service;
  }

  /**
   * @return service URI relative to the SOAP root
   */
  public String getPath () {

    return path;
  }

  /**
   * @param path service URI relative to the SOAP root
   */
  public void setPath (String path) {

    this.path = path;
  }

  /**
   * @return service instance to expose
   */
  public Object getService () {

    return service;
  }

  /**
   * @param service service instance to expose
   */
  public void setService (Object service) {

    this.service = service;
  }

  /**
   * @return whether the SOAP handler should be marked async supported; {@code null} leaves default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * @param asyncSupported flag indicating async support
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
