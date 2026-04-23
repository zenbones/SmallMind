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

import java.util.Map;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Wire signal that carries a service invocation request, including the target route,
 * named argument map, propagated contexts, and a flag indicating whether a response is expected.
 */
@XmlRootElement(name = "invocation", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class InvocationSignal implements Signal {

  private Route route;
  private Map<String, Object> arguments;
  private WireContext[] contexts;
  private boolean inOnly;

  /**
   * No-argument constructor required by JAXB.
   */
  public InvocationSignal () {

  }

  /**
   * Creates a fully populated invocation signal.
   *
   * @param inOnly    {@code true} if the call is one-way and no response is expected
   * @param route     the target route identifying service, version, and function
   * @param arguments named arguments to pass to the remote function
   * @param contexts  zero or more wire contexts to propagate with the invocation
   */
  public InvocationSignal (boolean inOnly, Route route, Map<String, Object> arguments, WireContext... contexts) {

    this.inOnly = inOnly;
    this.route = route;
    this.arguments = arguments;
    this.contexts = contexts;
  }

  /**
   * Returns {@code true} when the caller does not expect a response to this invocation.
   *
   * @return {@code true} for one-way (fire-and-forget) calls
   */
  @XmlElement(name = "inOnly")
  public boolean isInOnly () {

    return inOnly;
  }

  /**
   * Sets whether the invocation is one-way (no response expected).
   *
   * @param inOnly {@code true} to mark the call as one-way
   */
  public void setInOnly (boolean inOnly) {

    this.inOnly = inOnly;
  }

  /**
   * Returns the routing information for this invocation.
   *
   * @return the target {@link Route}
   */
  @XmlElementRef
  public Route getRoute () {

    return route;
  }

  /**
   * Sets the routing information for this invocation.
   *
   * @param route the target {@link Route}
   */
  public void setRoute (Route route) {

    this.route = route;
  }

  /**
   * Returns the wire contexts propagated with this invocation, or {@code null} if none were set.
   *
   * @return array of {@link WireContext} instances, or {@code null}
   */
  @XmlJavaTypeAdapter(WireContextXmlAdapter.class)
  @XmlElement(name = "contexts")
  public WireContext[] getContexts () {

    return contexts;
  }

  /**
   * Sets the wire contexts to propagate with this invocation.
   *
   * @param contexts contexts to attach to the signal
   */
  public void setContexts (WireContext[] contexts) {

    this.contexts = contexts;
  }

  /**
   * Returns the map of named arguments to pass to the remote function.
   *
   * @return argument map keyed by parameter name
   */
  @XmlElement(name = "arguments")
  public Map<String, Object> getArguments () {

    return arguments;
  }

  /**
   * Sets the map of named arguments for the remote function.
   *
   * @param arguments argument map keyed by parameter name
   */
  public void setArguments (Map<String, Object> arguments) {

    this.arguments = arguments;
  }
}
