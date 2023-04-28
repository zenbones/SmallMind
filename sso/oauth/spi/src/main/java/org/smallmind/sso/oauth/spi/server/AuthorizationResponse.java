package org.smallmind.sso.oauth.spi.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AuthorizationResponse {

  private final String code;
  private final String state;

  public AuthorizationResponse (String code) {

    this(code, null);
  }

  public AuthorizationResponse (String code, String state) {

    this.code = code;
    this.state = state;
  }

  @XmlElement(name = "code", required = true)
  public String getCode () {

    return code;
  }

  @XmlElement(name = "state")
  public String getState () {

    return state;
  }
}
