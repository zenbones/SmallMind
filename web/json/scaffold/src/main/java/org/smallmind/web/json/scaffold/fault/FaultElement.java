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
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "element", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class FaultElement implements Serializable {

  private String declaringType;
  private String functionName;
  private String fileName;
  private int lineNumber;

  public FaultElement () {

  }

  public FaultElement (String declaringType, String functionName) {

    this.declaringType = declaringType;
    this.functionName = functionName;

    lineNumber = -1;
  }

  public FaultElement (String declaringType, String functionName, String fileName, int lineNumber) {

    this.declaringType = declaringType;
    this.functionName = functionName;
    this.fileName = fileName;
    this.lineNumber = lineNumber;
  }

  public FaultElement (StackTraceElement stackTraceElement) {

    declaringType = stackTraceElement.getClassName();
    functionName = stackTraceElement.getMethodName();
    fileName = stackTraceElement.getFileName();
    lineNumber = stackTraceElement.getLineNumber();
  }

  @XmlElement(name = "type", required = true)
  public String getDeclaringType () {

    return declaringType;
  }

  public void setDeclaringType (String declaringType) {

    this.declaringType = declaringType;
  }

  @XmlElement(name = "function", required = true)
  public String getFunctionName () {

    return functionName;
  }

  public void setFunctionName (String functionName) {

    this.functionName = functionName;
  }

  @XmlElement(name = "file", required = true)
  public String getFileName () {

    return fileName;
  }

  public void setFileName (String fileName) {

    this.fileName = fileName;
  }

  @XmlElement(name = "line", required = true)
  public int getLineNumber () {

    return lineNumber;
  }

  public void setLineNumber (int lineNumber) {

    this.lineNumber = lineNumber;
  }

  public String toString () {

    StringBuilder prettyBuilder = new StringBuilder(declaringType);

    prettyBuilder.append('.').append(functionName);
    if ((fileName != null) && (lineNumber >= 0)) {
      prettyBuilder.append('(').append(fileName).append(':').append(lineNumber).append(')');
    } else if (fileName != null) {
      prettyBuilder.append('(').append(fileName).append(')');
    } else {
      prettyBuilder.append("(Unknown Source)");
    }

    return prettyBuilder.toString();
  }

  public int hashCode () {

    return Objects.hash(declaringType, functionName, fileName, lineNumber);
  }

  public boolean equals (Object obj) {

    return (obj == this) || ((obj instanceof FaultElement) && Objects.equals(((FaultElement)obj).getDeclaringType(), declaringType) && Objects.equals(((FaultElement)obj).getFunctionName(), functionName) && Objects.equals(((FaultElement)obj).getFileName(), fileName) && (((FaultElement)obj).getLineNumber() == lineNumber));
  }
}