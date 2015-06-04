package org.smallmind.phalanx.wire;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "address")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Address implements Serializable {

  private Location location;

  public Address () {

  }

  public Address (Location location) {

    this.location = location;
  }

  @XmlElementRefs({@XmlElementRef(type = TalkLocation.class), @XmlElementRef(type = WhisperLocation.class)})
  public Location getLocation () {

    return location;
  }

  public void setLocation (Location location) {

    this.location = location;
  }
}