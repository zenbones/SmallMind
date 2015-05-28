package org.smallmind.throng.wire;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "talk")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TalkLocation extends Location {

  private Function function;
  private String service;
  private int version;

  public TalkLocation () {

  }

  public TalkLocation (int version, String service, Function function) {

    this.version = version;
    this.service = service;
    this.function = function;
  }

  @Override
  @XmlTransient
  public LocationType getType () {

    return LocationType.TALK;
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
