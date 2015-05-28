package org.smallmind.throng.wire;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "test")
public class TestWireContext extends WireContext {

  private String message;

  public TestWireContext () {

  }

  public TestWireContext (String message) {

    this.message = message;
  }

  @XmlElement(name = "message", required = true, nillable = false)
  public String getMessage () {

    return message;
  }

  public void setMessage (String message) {

    this.message = message;
  }
}
