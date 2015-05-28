package org.smallmind.throng.wire;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ResultSignal implements Signal {

  private Object result;
  private String nativeType;
  private boolean error;

  public ResultSignal () {

  }

  public ResultSignal (boolean error, String nativeType, Object result) {

    this.error = error;
    this.nativeType = nativeType;
    this.result = result;
  }

  @XmlElement(name = "error", required = true, nillable = false)
  public boolean isError () {

    return error;
  }

  public void setError (boolean error) {

    this.error = error;
  }

  @XmlElement(name = "nativeType", required = true, nillable = false)
  public String getNativeType () {

    return nativeType;
  }

  public void setNativeType (String nativeType) {

    this.nativeType = nativeType;
  }

  @XmlElement(name = "result", required = true, nillable = false)
  public Object getResult () {

    return result;
  }

  public void setResult (Object result) {

    this.result = result;
  }
}
