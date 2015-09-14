package org.smallmind.phalanx.wire;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "address")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Address implements Serializable {

  private Function function;
  private String service;
  private int version;

  public Address () {

  }

  public Address (int version, String service, Function function) {

    this.version = version;
    this.service = service;
    this.function = function;
  }

  @XmlElement(name = "version", required = true, nillable = false)
  public int getVersion () {

    return version;
  }

  public void setVersion (int version) {

    this.version = version;
  }

  @XmlElement(name = "service", required = true, nillable = false)
  public String getService () {

    return service;
  }

  public void setService (String service) {

    this.service = service;
  }

  @XmlElementRef(required = true)
  public Function getFunction () {

    return function;
  }

  public void setFunction (Function function) {

    this.function = function;
  }
}