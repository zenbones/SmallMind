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

import java.io.IOException;
import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Structured fault representation that can be serialized to JSON/XML, carrying stack trace,
 * nested causes, and optional native/extra information.
 */
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

  /**
   * No-arg constructor for frameworks.
   */
  public Fault () {

  }

  /**
   * Creates a fault with only a message.
   *
   * @param message human-readable message
   */
  public Fault (String message) {

    this.message = message;
  }

  /**
   * Creates a fault with contextual location and message.
   *
   * @param context context element describing where the fault occurred
   * @param message human-readable message
   */
  public Fault (FaultElement context, String message) {

    this(message);

    this.context = context;
  }

  /**
   * Creates a fault from a throwable, capturing stack trace and optional native encoding.
   *
   * @param throwable             thrown exception
   * @param includeNativeEncoding whether to serialize the exception natively
   */
  public Fault (Throwable throwable) {

    this(null, throwable, true);
  }

  /**
   * Creates a contextual fault from a throwable, capturing stack trace and optional native encoding.
   *
   * @param context               context element describing the call site
   * @param throwable             thrown exception
   * @param includeNativeEncoding whether to serialize the exception natively
   */
  public Fault (FaultElement context, Throwable throwable) {

    this(context, throwable, true);
  }

  /**
   * Creates a fault from a throwable with control over native encoding.
   *
   * @param throwable             thrown exception
   * @param includeNativeEncoding whether to serialize the exception natively
   */
  public Fault (Throwable throwable, boolean includeNativeEncoding) {

    this(null, throwable, includeNativeEncoding);
  }

  /**
   * Creates a contextual fault from a throwable with control over native encoding.
   *
   * @param context               context element describing the call site
   * @param throwable             thrown exception
   * @param includeNativeEncoding whether to serialize the exception natively
   */
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

  /**
   * Computes how many trailing elements of the previous trace are repeated by the current element.
   *
   * @param element   current fault element
   * @param prevTrace prior stack trace to compare against
   * @return number of repeated elements from the previous trace, or -1 if none match
   */
  private static int findRepeatedStackElements (FaultElement element, FaultElement[] prevTrace) {

    for (int count = 0; count < prevTrace.length; count++) {
      if (element.equals(prevTrace[count])) {

        return prevTrace.length - count;
      }
    }

    return -1;
  }

  /**
   * @return contextual element indicating the origin of the fault
   */
  @XmlElement(name = "context")
  public FaultElement getContext () {

    return context;
  }

  /**
   * Sets the contextual element indicating the origin of the fault.
   *
   * @param context context element
   */
  public void setContext (FaultElement context) {

    this.context = context;
  }

  /**
   * @return fully qualified throwable type name
   */
  @XmlElement(name = "type")
  public String getThrowableType () {

    return throwableType;
  }

  /**
   * Sets the fully qualified throwable type name.
   *
   * @param throwableType throwable class name
   */
  public void setThrowableType (String throwableType) {

    this.throwableType = throwableType;
  }

  /**
   * @return fault message
   */
  @XmlElement(name = "message")
  public String getMessage () {

    return message;
  }

  /**
   * Sets the fault message.
   *
   * @param message human-readable message
   */
  public void setMessage (String message) {

    this.message = message;
  }

  /**
   * @return nested cause fault, if any
   */
  @XmlElement(name = "cause")
  public Fault getCause () {

    return cause;
  }

  /**
   * Sets the nested cause fault.
   *
   * @param cause nested fault
   */
  public void setCause (Fault cause) {

    this.cause = cause;
  }

  /**
   * @return captured stack trace elements
   */
  @XmlElement(name = "trace")
  public FaultElement[] getElements () {

    return elements;
  }

  /**
   * Sets the captured stack trace elements.
   *
   * @param elements stack trace elements
   */
  public void setElements (FaultElement[] elements) {

    this.elements = elements;
  }

  /**
   * @return structured auxiliary information (may be {@code null})
   */
  @XmlElement(name = "information")
  public ObjectNode getInformation () {

    return information;
  }

  /**
   * Sets structured auxiliary information.
   *
   * @param information JSON node containing metadata
   */
  public void setInformation (ObjectNode information) {

    this.information = information;
  }

  /**
   * @return optional native representation of the faulting object
   */
  @XmlElement(name = "native")
  public NativeObject getNativeObject () {

    return nativeObject;
  }

  /**
   * Sets the native representation of the faulting object.
   *
   * @param nativeObject serialized native payload
   */
  public void setNativeObject (NativeObject nativeObject) {

    this.nativeObject = nativeObject;
  }

  /**
   * Renders the fault and its causes into a multi-line human-readable string.
   *
   * @return formatted fault text
   */
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
