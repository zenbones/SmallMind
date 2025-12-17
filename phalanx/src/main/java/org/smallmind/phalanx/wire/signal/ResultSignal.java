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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Signal that carries a return value or error indicator back to the caller.
 */
@XmlRootElement(name = "result", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ResultSignal implements Signal {

  private Object result;
  private String nativeType;
  private boolean error;

  /**
   * Default constructor for JAXB.
   */
  public ResultSignal () {

  }

  /**
   * Creates a populated result signal.
   *
   * @param error      {@code true} when the result represents an error payload
   * @param nativeType JVM descriptor for the result type
   * @param result     result object or error value
   */
  public ResultSignal (boolean error, String nativeType, Object result) {

    this.error = error;
    this.nativeType = nativeType;
    this.result = result;
  }

  /**
   * Indicates whether the result represents an error.
   *
   * @return {@code true} when the invocation failed
   */
  @XmlElement(name = "error", required = true)
  public boolean isError () {

    return error;
  }

  /**
   * Sets the error flag.
   *
   * @param error {@code true} if the payload is an error
   */
  public void setError (boolean error) {

    this.error = error;
  }

  /**
   * Returns the JVM descriptor for the result type.
   *
   * @return native type descriptor
   */
  @XmlElement(name = "nativeType", required = true)
  public String getNativeType () {

    return nativeType;
  }

  /**
   * Sets the JVM descriptor for the result type.
   *
   * @param nativeType native type descriptor
   */
  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  /**
   * Returns the result value or error payload.
   *
   * @return result object
   */
  @XmlElement(name = "result", required = true)
  public Object getResult () {

    return result;
  }

  /**
   * Sets the result value or error payload.
   *
   * @param result result object
   */
  public void setResult (Object result) {

    this.result = result;
  }
}
