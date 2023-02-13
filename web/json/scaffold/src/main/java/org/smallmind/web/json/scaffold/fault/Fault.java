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

import java.io.IOException;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.scribe.pen.LoggerManager;

@XmlRootElement(name = "fault", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Fault implements Serializable, Informed {

  private Fault cause;
  private FaultElement context;
  private FaultElement[] elements;
  private ObjectNode information;
  private String throwableType;
  private String message;
  private NativeObject nativeObject;

  public Fault () {

  }

  public Fault (String message) {

    this.message = message;
  }

  public Fault (FaultElement context, String message) {

    this(message);

    this.context = context;
  }

  public Fault (Throwable throwable) {

    this(null, throwable, true);
  }

  public Fault (FaultElement context, Throwable throwable) {

    this(context, throwable, true);
  }

  public Fault (Throwable throwable, boolean includeNativeEncoding) {

    this(null, throwable, includeNativeEncoding);
  }

  public Fault (FaultElement context, Throwable throwable, boolean includeNativeEncoding) {

    this.context = context;

    StackTraceElement[] stackTraceElements;
    int index = 0;

    if (includeNativeEncoding) {
      try {
        nativeObject = new NativeObject(throwable);
      } catch (IOException ioException) {
        LoggerManager.getLogger(Fault.class).error(ioException.initCause(new NativeObjectException(throwable)));
      }
    }

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

    if (throwable instanceof Informed) {
      information = ((Informed)throwable).getInformation();
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

  @XmlElement(name = "context")
  public FaultElement getContext () {

    return context;
  }

  public void setContext (FaultElement context) {

    this.context = context;
  }

  @XmlElement(name = "type")
  public String getThrowableType () {

    return throwableType;
  }

  public void setThrowableType (String throwableType) {

    this.throwableType = throwableType;
  }

  @XmlElement(name = "message")
  public String getMessage () {

    return message;
  }

  public void setMessage (String message) {

    this.message = message;
  }

  @XmlElement(name = "cause")
  public Fault getCause () {

    return cause;
  }

  public void setCause (Fault cause) {

    this.cause = cause;
  }

  @XmlElement(name = "trace")
  public FaultElement[] getElements () {

    return elements;
  }

  public void setElements (FaultElement[] elements) {

    this.elements = elements;
  }

  @XmlElement(name = "information")
  public ObjectNode getInformation () {

    return information;
  }

  public void setInformation (ObjectNode information) {

    this.information = information;
  }

  @XmlElement(name = "native")
  public NativeObject getNativeObject () {

    return nativeObject;
  }

  public void setNativeObject (NativeObject nativeObject) {

    this.nativeObject = nativeObject;
  }

  @Override
  public String toString () {

    StringBuilder lineBuilder = new StringBuilder("Error in process ");

    if (context != null) {
      lineBuilder.append("at ").append(context).append(' ');
    }

    return print(lineBuilder);
  }

  private String print (StringBuilder lineBuilder) {

    Fault fault = this;
    FaultElement[] prevTrace = null;
    StringBuilder traceBuilder;
    int repeatedElements;

    traceBuilder = new StringBuilder();

    do {

      String throwableType;

      if (prevTrace != null) {
        lineBuilder.append("Caused by: ");
      }

      lineBuilder.append(((throwableType = fault.getThrowableType()) != null) ? throwableType : "unknown");
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
