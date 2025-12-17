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

@XmlRootElement(name = "invocation", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
/**
 * Signal carrying a request invocation, including routing information, arguments, and contexts.
 */
public class InvocationSignal implements Signal {

  private Route route;
  private Map<String, Object> arguments;
  private WireContext[] contexts;
  private boolean inOnly;

  /**
   * Default constructor for JAXB.
   */
  public InvocationSignal () {

  }

  /**
   * Creates an invocation signal populated with route, arguments, and contexts.
   *
   * @param inOnly    whether the invocation is one-way
   * @param route     destination route
   * @param arguments argument map keyed by name
   * @param contexts  optional wire contexts to propagate
   */
  public InvocationSignal (boolean inOnly, Route route, Map<String, Object> arguments, WireContext... contexts) {

    this.inOnly = inOnly;
    this.route = route;
    this.arguments = arguments;
    this.contexts = contexts;
  }

  /**
   * Indicates whether the invocation expects no response.
   *
   * @return {@code true} for one-way calls
   */
  @XmlElement(name = "inOnly")
  public boolean isInOnly () {

    return inOnly;
  }

  /**
   * Sets the one-way flag for the invocation.
   *
   * @param inOnly {@code true} when no response is expected
   */
  public void setInOnly (boolean inOnly) {

    this.inOnly = inOnly;
  }

  /**
   * Returns the target route.
   *
   * @return route information
   */
  @XmlElementRef
  public Route getRoute () {

    return route;
  }

  /**
   * Updates the target route for the signal.
   *
   * @param route new route
   */
  public void setRoute (Route route) {

    this.route = route;
  }

  /**
   * Returns the propagated wire contexts.
   *
   * @return array of contexts or {@code null}
   */
  @XmlJavaTypeAdapter(WireContextXmlAdapter.class)
  @XmlElement(name = "contexts")
  public WireContext[] getContexts () {

    return contexts;
  }

  /**
   * Sets the wire contexts associated with the invocation.
   *
   * @param contexts contexts to attach
   */
  public void setContexts (WireContext[] contexts) {

    this.contexts = contexts;
  }

  /**
   * Returns the argument payload keyed by name.
   *
   * @return argument map
   */
  @XmlElement(name = "arguments")
  public Map<String, Object> getArguments () {

    return arguments;
  }

  /**
   * Sets the argument payload keyed by name.
   *
   * @param arguments argument map
   */
  public void setArguments (Map<String, Object> arguments) {

    this.arguments = arguments;
  }
}
