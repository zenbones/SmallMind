package org.smallmind.phalanx.wire;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "function")
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
    resultType = ((result = method.getAnnotation(Result.class)) == null) ? TypeUtility.neutralEncode(method.getReturnType()) : "!" + result.value();

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
        signature[index] = TypeUtility.neutralEncode(parameterClasses[index]);
      }
    }

    nativeType = TypeUtility.nativeEncode(method.getReturnType());
  }

  @XmlTransient
  public boolean isPartial () {

    return (signature == null) || (resultType == null);
  }

  @XmlElement(name = "name", required = true, nillable = false)
  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  @XmlElement(name = "nativeType", required = false, nillable = false)
  public String getNativeType () {

    return nativeType;
  }

  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  @XmlElement(name = "resultType", required = false, nillable = false)
  public String getResultType () {

    return resultType;
  }

  public void setResultType (String resultType) {

    this.resultType = resultType;
  }

  @XmlElement(name = "signature", required = false, nillable = false)
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