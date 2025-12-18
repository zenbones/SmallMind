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
package org.smallmind.web.json.scaffold.fault;

import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Structured payload conveying a code, template, and argument list for rendering user-facing fault messages.
 */
@XmlRootElement(name = "information", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class FaultInformation implements Serializable {

  private ArrayNode arguments;
  private String template;
  private int code;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public FaultInformation () {

  }

  /**
   * Creates a fault information payload with the provided template and arguments.
   *
   * @param code      fault code identifier
   * @param template  message template
   * @param arguments template arguments
   */
  public FaultInformation (int code, String template, Object... arguments) {

    this.code = code;
    this.template = template;
    this.arguments = (ArrayNode)JsonCodec.writeAsJsonNode(arguments);
  }

  /**
   * @return numeric fault code
   */
  @XmlElement(name = "code")
  public int getCode () {

    return code;
  }

  /**
   * Sets the numeric fault code.
   *
   * @param code fault code identifier
   */
  public void setCode (int code) {

    this.code = code;
  }

  /**
   * @return message template string
   */
  @XmlElement(name = "template")
  public String getTemplate () {

    return template;
  }

  /**
   * Sets the message template.
   *
   * @param template message template string
   */
  public void setTemplate (String template) {

    this.template = template;
  }

  /**
   * Converts the argument list into strongly-typed objects based on the supplied class array.
   *
   * @param classes target classes for conversion
   * @return converted arguments
   * @throws IllegalArgumentException if the number of classes does not match the number of arguments
   */
  @XmlTransient
  public Object[] getArgumentsAs (Class<?>[] classes) {

    if ((arguments == null) || (((classes == null) ? 0 : classes.length) != arguments.size())) {
      throw new IllegalArgumentException("The number of classes(" + ((classes == null) ? 0 : classes.length) + ") must match the number of arguments(" + ((arguments == null) ? 0 : arguments.size()) + ")");
    }

    Object[] convertedArguments = new Object[arguments.size()];

    for (int index = 0; index < arguments.size(); index++) {
      convertedArguments[index] = JsonCodec.convert(arguments.get(index), classes[index]);
    }

    return convertedArguments;
  }

  /**
   * Converts the argument at the specified index into the requested type.
   *
   * @param index argument position
   * @param clazz target class
   * @param <T>   target type
   * @return converted argument
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  @XmlTransient
  public <T> T getArgumentAs (int index, Class<T> clazz) {

    if ((index < 0) || (arguments == null) || (index >= arguments.size())) {
      throw new IndexOutOfBoundsException(index + ">" + ((arguments == null) ? 0 : arguments.size()));
    }

    return JsonCodec.convert(arguments.get(index), clazz);
  }

  /**
   * @return raw arguments as a JSON array
   */
  @XmlElement(name = "arguments")
  public ArrayNode getArguments () {

    return arguments;
  }

  /**
   * Sets the raw argument array.
   *
   * @param arguments JSON array of arguments
   */
  public void setArguments (ArrayNode arguments) {

    this.arguments = arguments;
  }
}
