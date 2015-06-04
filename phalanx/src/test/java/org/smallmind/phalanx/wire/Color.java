package org.smallmind.phalanx.wire;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "color")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Color {

  private String color;

  public Color () {

  }

  public Color (String color) {

    this.color = color;
  }

  @XmlElement(name = "color", required = true, nillable = false)
  public String getColor () {

    return color;
  }

  public void setColor (String color) {

    this.color = color;
  }

  @Override
  public int hashCode () {

    return color.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Color) && ((Color)obj).getColor().equals(color);
  }
}