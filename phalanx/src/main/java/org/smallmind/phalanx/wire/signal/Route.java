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
package org.smallmind.phalanx.wire.signal;

import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Serializable routing address that combines a service name, API version, and target
 * {@link Function} descriptor to fully identify the endpoint of a wire invocation.
 */
@XmlRootElement(name = "address", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Route implements Serializable {

  private Function function;
  private String service;
  private int version;

  /**
   * No-argument constructor required by JAXB.
   */
  public Route () {

  }

  /**
   * Creates a fully populated route.
   *
   * @param version  the integer version of the target service API
   * @param service  the logical name of the target service
   * @param function the function descriptor identifying which operation to invoke
   */
  public Route (int version, String service, Function function) {

    this.version = version;
    this.service = service;
    this.function = function;
  }

  /**
   * Returns the integer API version of the target service.
   *
   * @return the service version number
   */
  @XmlElement(name = "version", required = true)
  public int getVersion () {

    return version;
  }

  /**
   * Sets the integer API version of the target service.
   *
   * @param version the service version number
   */
  public void setVersion (int version) {

    this.version = version;
  }

  /**
   * Returns the logical name of the target service.
   *
   * @return the service name
   */
  @XmlElement(name = "service", required = true)
  public String getService () {

    return service;
  }

  /**
   * Sets the logical name of the target service.
   *
   * @param service the service name
   */
  public void setService (String service) {

    this.service = service;
  }

  /**
   * Returns the {@link Function} descriptor that identifies the operation to invoke.
   *
   * @return the function descriptor
   */
  @XmlElementRef
  public Function getFunction () {

    return function;
  }

  /**
   * Sets the {@link Function} descriptor that identifies the operation to invoke.
   *
   * @param function the function descriptor
   */
  public void setFunction (Function function) {

    this.function = function;
  }
}
