package org.smallmind.phalanx.wire;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "invocation")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class InvocationSignal implements Signal {

  private Address address;
  private Map<String, Object> arguments;
  private WireContext[] contexts;
  private boolean inOnly;

  public InvocationSignal () {

  }

  public InvocationSignal (boolean inOnly, Address address, Map<String, Object> arguments, WireContext... contexts) {

    this.inOnly = inOnly;
    this.address = address;
    this.arguments = arguments;
    this.contexts = contexts;
  }

  @XmlElement(name = "inOnly", required = false, nillable = false)
  public boolean isInOnly () {

    return inOnly;
  }

  public void setInOnly (boolean inOnly) {

    this.inOnly = inOnly;
  }

  @XmlElementRef(required = true)
  public Address getAddress () {

    return address;
  }

  public void setAddress (Address address) {

    this.address = address;
  }

  @XmlJavaTypeAdapter(WireContextXmlAdapter.class)
  @XmlElement(name = "contexts", required = false, nillable = false)
  public WireContext[] getContexts () {

    return contexts;
  }

  public void setContexts (WireContext[] contexts) {

    this.contexts = contexts;
  }

  @XmlElement(name = "arguments", required = false, nillable = false)
  public Map<String, Object> getArguments () {

    return arguments;
  }

  public void setArguments (Map<String, Object> arguments) {

    this.arguments = arguments;
  }
}
