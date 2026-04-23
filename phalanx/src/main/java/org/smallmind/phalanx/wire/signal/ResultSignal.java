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
 * Wire signal that carries the outcome of a remote invocation back to the caller, holding
 * the return value (or error payload), the JVM native type descriptor, and an error flag.
 */
@XmlRootElement(name = "result", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ResultSignal implements Signal {

  private Object result;
  private String nativeType;
  private boolean error;

  /**
   * No-argument constructor required by JAXB.
   */
  public ResultSignal () {

  }

  /**
   * Creates a fully populated result signal.
   *
   * @param error      {@code true} when the invocation failed and {@code result} is an error object
   * @param nativeType JVM descriptor string for the declared return type of the invoked method
   * @param result     the return value on success, or the error object on failure
   */
  public ResultSignal (boolean error, String nativeType, Object result) {

    this.error = error;
    this.nativeType = nativeType;
    this.result = result;
  }

  /**
   * Returns {@code true} when the remote invocation failed and {@code result} contains an error payload.
   *
   * @return {@code true} if this signal represents an error
   */
  @XmlElement(name = "error", required = true)
  public boolean isError () {

    return error;
  }

  /**
   * Sets the error flag.
   *
   * @param error {@code true} if the payload is an error; {@code false} for a normal return value
   */
  public void setError (boolean error) {

    this.error = error;
  }

  /**
   * Returns the JVM descriptor string for the declared return type of the invoked method
   * (e.g., {@code Ljava/lang/String;} or {@code V}).
   *
   * @return the native type descriptor
   */
  @XmlElement(name = "nativeType", required = true)
  public String getNativeType () {

    return nativeType;
  }

  /**
   * Sets the JVM descriptor string for the declared return type.
   *
   * @param nativeType the native type descriptor string
   */
  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  /**
   * Returns the result value on a successful invocation, or the error object when
   * {@link #isError()} is {@code true}.
   *
   * @return the result or error payload
   */
  @XmlElement(name = "result", required = true)
  public Object getResult () {

    return result;
  }

  /**
   * Sets the result value or error payload for this signal.
   *
   * @param result the return value or error object to carry
   */
  public void setResult (Object result) {

    this.result = result;
  }
}
