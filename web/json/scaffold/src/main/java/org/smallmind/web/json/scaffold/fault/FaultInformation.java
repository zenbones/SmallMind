/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.smallmind.web.json.scaffold.util.JsonCodec;

@XmlRootElement(name = "information", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class FaultInformation implements Serializable {

  private ArrayNode arguments;
  private String template;
  private int code;

  public FaultInformation () {

  }

  public FaultInformation (int code, String template, Object... arguments) {

    this.code = code;
    this.template = template;
    this.arguments = (ArrayNode)JsonCodec.writeAsJsonNode(arguments);
  }

  @XmlElement(name = "code")
  public int getCode () {

    return code;
  }

  public void setCode (int code) {

    this.code = code;
  }

  @XmlElement(name = "template")
  public String getTemplate () {

    return template;
  }

  public void setTemplate (String template) {

    this.template = template;
  }

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

  @XmlTransient
  public <T> T getArgumentAs (int index, Class<T> clazz) {

    if ((index < 0) || (arguments == null) || (index >= arguments.size())) {
      throw new IndexOutOfBoundsException(index + ">" + ((arguments == null) ? 0 : arguments.size()));
    }

    return JsonCodec.convert(arguments.get(index), clazz);
  }

  @XmlElement(name = "arguments")
  public ArrayNode getArguments () {

    return arguments;
  }

  public void setArguments (ArrayNode arguments) {

    this.arguments = arguments;
  }
}
