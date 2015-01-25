/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.jersey.fault;

import java.io.IOException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Fault {

  private Fault cause;
  private FaultElement[] elements;
  private String throwableType;
  private String message;
  private NativeObject nativeObject;

  public Fault () {

  }

  public Fault (Throwable throwable)
    throws IOException {

    StackTraceElement[] stackTraceElements;
    int index = 0;

    nativeObject = new NativeObject(throwable);

    throwableType = throwable.getClass().getName();
    message = throwable.getMessage();

    if ((stackTraceElements = throwable.getStackTrace()) != null) {
      elements = new FaultElement[stackTraceElements.length];
      for (StackTraceElement stackTraceElement : stackTraceElements) {
        elements[index++] = new FaultElement(stackTraceElement);
      }
    }

    if (throwable.getCause() != null) {
      cause = new Fault(throwable.getCause());
    }
  }

  private static int findRepeatedStackElements (FaultElement element, FaultElement[] prevTrace) {

    for (int count = 0; count < prevTrace.length; count++) {
      if (element.equals(prevTrace[count])) {

        return prevTrace.length - count;
      }
    }

    return -1;
  }

  @XmlElement(name = "type", required = false, nillable = false)
  public String getThrowableType () {

    return throwableType;
  }

  public void setThrowableType (String throwableType) {

    this.throwableType = throwableType;
  }

  @XmlElement(name = "message", required = false, nillable = false)
  public String getMessage () {

    return message;
  }

  public void setMessage (String message) {

    this.message = message;
  }

  @XmlElement(name = "cause", required = false, nillable = false)
  public Fault getCause () {

    return cause;
  }

  public void setCause (Fault cause) {

    this.cause = cause;
  }

  @XmlElement(name = "trace", required = false, nillable = false)
  public FaultElement[] getElements () {

    return elements;
  }

  public void setElements (FaultElement[] elements) {

    this.elements = elements;
  }

  @XmlElement(name = "native", required = false, nillable = false)
  public NativeObject getNativeObject () {

    return nativeObject;
  }

  public void setNativeObject (NativeObject nativeObject) {

    this.nativeObject = nativeObject;
  }

  @Override
  public String toString () {

    StringBuilder lineBuilder = new StringBuilder();

    return print(lineBuilder.append("Error in process "));
  }

  private String print (StringBuilder lineBuilder) {

    Fault fault = this;
    FaultElement[] prevTrace = null;
    StringBuilder traceBuilder;
    int repeatedElements;

    traceBuilder = new StringBuilder();

    do {
      if (prevTrace != null) {
        lineBuilder.append("Caused by: ");
      }

      lineBuilder.append(fault.getThrowableType());
      lineBuilder.append(": ");
      lineBuilder.append(fault.getMessage());
      traceBuilder.append(lineBuilder).append(System.getProperty("line.separator"));
      lineBuilder.delete(0, lineBuilder.length());

      if (fault.getElements() != null) {
        for (FaultElement element : fault.getElements()) {
          if (prevTrace != null) {
            if ((repeatedElements = findRepeatedStackElements(element, prevTrace)) >= 0) {
              lineBuilder.append("   ... ");
              lineBuilder.append(repeatedElements);
              lineBuilder.append(" more");
              traceBuilder.append(lineBuilder).append(System.getProperty("line.separator"));
              lineBuilder.delete(0, lineBuilder.length());
              break;
            }
          }

          lineBuilder.append("   at ");
          lineBuilder.append(element);
          traceBuilder.append(lineBuilder).append(System.getProperty("line.separator"));
          lineBuilder.delete(0, lineBuilder.length());
        }
      }

      prevTrace = fault.getElements();
    } while ((fault = fault.getCause()) != null);

    return traceBuilder.toString();
  }
}