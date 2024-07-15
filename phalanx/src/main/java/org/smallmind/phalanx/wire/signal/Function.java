/*
 * Copyright (c) 2007 through 2024 David Berkman
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

@XmlRootElement(name = "function", namespace = "http://org.smallmind/phalanx/wire")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Function implements Serializable {

  private String name;
  private String resultType;
  private String nativeType;
  private String[] signature;

  public Function () {

  }

  public Function (String name) {

    this.name = name;
  }

  public Function (String name, String nativeType) {

    this.name = name;
    this.nativeType = nativeType;
  }

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

  @XmlTransient
  public boolean isPartial () {

    return (signature == null) || (resultType == null);
  }

  @XmlElement(name = "name", required = true)
  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  @XmlElement(name = "nativeType")
  public String getNativeType () {

    return nativeType;
  }

  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  @XmlElement(name = "resultType")
  public String getResultType () {

    return resultType;
  }

  public void setResultType (String resultType) {

    this.resultType = resultType;
  }

  @XmlElement(name = "signature")
  public String[] getSignature () {

    return signature;
  }

  public void setSignature (String[] signature) {

    this.signature = signature;
  }

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

  public boolean equals (Object obj) {

    return (obj instanceof Function) && name.equals(((Function)obj).getName()) && ((resultType == null) ? ((Function)obj).getResultType() == null : resultType.equals(((Function)obj).getResultType())) && Arrays.equals(signature, ((Function)obj).getSignature());
  }
}
