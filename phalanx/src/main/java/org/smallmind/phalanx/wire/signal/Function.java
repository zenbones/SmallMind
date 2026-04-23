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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.smallmind.phalanx.wire.Argument;
import org.smallmind.phalanx.wire.CallAs;
import org.smallmind.phalanx.wire.Result;
import org.smallmind.phalanx.wire.SignatureUtility;

/**
 * Serializable descriptor of a callable wire function, capturing its name, parameter signature,
 * neutral result type, and native JVM return-type encoding.
 */
@XmlRootElement(name = "function", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Function implements Serializable {

  private String name;
  private String resultType;
  private String nativeType;
  private String[] signature;

  /**
   * No-argument constructor required by JAXB.
   */
  public Function () {

  }

  /**
   * Creates a partial descriptor containing only the function name.
   *
   * @param name function name
   */
  public Function (String name) {

    this.name = name;
  }

  /**
   * Creates a descriptor with a function name and native return-type encoding.
   *
   * @param name       function name
   * @param nativeType JVM descriptor string for the return type
   */
  public Function (String name, String nativeType) {

    this.name = name;
    this.nativeType = nativeType;
  }

  /**
   * Builds a complete descriptor from a reflected method, deriving the name from
   * {@link org.smallmind.phalanx.wire.CallAs} when present, the result type from
   * {@link org.smallmind.phalanx.wire.Result} when present, and the parameter signature
   * from {@link org.smallmind.phalanx.wire.Argument} type hints or neutral encoding.
   *
   * @param method the reflected method to describe
   */
  public Function (Method method) {

    Class[] parameterClasses;
    Annotation[][] parameterAnnotations;
    CallAs callAs;
    Result result;

    name = ((callAs = method.getAnnotation(CallAs.class)) == null) ? method.getName() : callAs.value();
    resultType = ((result = method.getAnnotation(Result.class)) == null) ? SignatureUtility.neutralEncode(method.getReturnType()) : "!" + result.value();

    signature = new String[(parameterClasses = method.getParameterTypes()).length];
    parameterAnnotations = method.getParameterAnnotations();
    for (int index = 0; index < parameterClasses.length; index++) {

      boolean viaAnnotation = false;

      for (Annotation annotation : parameterAnnotations[index]) {
        if (annotation.annotationType().equals(Argument.class)) {

          String typeHint;

          if ((typeHint = ((Argument)annotation).type()).length() > 0) {
            viaAnnotation = true;
            signature[index] = "!" + typeHint;
          }

          break;
        }
      }

      if (!viaAnnotation) {
        signature[index] = SignatureUtility.neutralEncode(parameterClasses[index]);
      }
    }

    nativeType = SignatureUtility.nativeEncode(method.getReturnType());
  }

  /**
   * Returns {@code true} when the signature or result type is absent, indicating an incomplete descriptor.
   *
   * @return {@code true} if either the signature or the result type is {@code null}
   */
  @XmlTransient
  public boolean isPartial () {

    return (signature == null) || (resultType == null);
  }

  /**
   * Returns the function name used on the wire.
   *
   * @return function name
   */
  @XmlElement(name = "name", required = true)
  public String getName () {

    return name;
  }

  /**
   * Sets the function name used on the wire.
   *
   * @param name function name
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Returns the JVM descriptor string for the return type (e.g., {@code Ljava/lang/String;}).
   *
   * @return native type descriptor, or {@code null} if not set
   */
  @XmlElement(name = "nativeType")
  public String getNativeType () {

    return nativeType;
  }

  /**
   * Sets the JVM descriptor string for the return type.
   *
   * @param nativeType native type descriptor string
   */
  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  /**
   * Returns the neutral or named result type encoding; a leading {@code !} denotes a named type alias.
   *
   * @return result type encoding, or {@code null} if not set
   */
  @XmlElement(name = "resultType")
  public String getResultType () {

    return resultType;
  }

  /**
   * Sets the neutral or named result type encoding.
   *
   * @param resultType result type encoding
   */
  public void setResultType (String resultType) {

    this.resultType = resultType;
  }

  /**
   * Returns the array of encoded parameter type strings that form the function's signature.
   *
   * @return parameter signature array, or {@code null} if not set
   */
  @XmlElement(name = "signature")
  public String[] getSignature () {

    return signature;
  }

  /**
   * Sets the array of encoded parameter type strings.
   *
   * @param signature parameter signature array
   */
  public void setSignature (String[] signature) {

    this.signature = signature;
  }

  /**
   * Returns a hash code derived from the name, result type, and each parameter type string.
   *
   * @return hash code for this descriptor
   */
  public int hashCode () {

    int hashCode;

    hashCode = name.hashCode();

    if (resultType != null) {
      hashCode = hashCode ^ resultType.hashCode();
    }

    if (signature != null) {
      for (String parameter : signature) {
        hashCode = hashCode ^ parameter.hashCode();
      }
    }

    return hashCode;
  }

  /**
   * Returns {@code true} when {@code obj} is a {@code Function} with the same name, result type,
   * and parameter signature array (compared element-by-element).
   *
   * @param obj the object to compare against
   * @return {@code true} if this descriptor is equal to {@code obj}
   */
  public boolean equals (Object obj) {

    return (obj instanceof Function) && name.equals(((Function)obj).getName()) && ((resultType == null) ? ((Function)obj).getResultType() == null : resultType.equals(((Function)obj).getResultType())) && Arrays.equals(signature, ((Function)obj).getSignature());
  }
}
