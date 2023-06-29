package org.smallmind.cometd.oumuamua.message;

import javax.xml.bind.annotation.XmlElement;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class HandshakeResponseMessage extends HandshakeMessage {

  private String[] supportedConnectionTypes;
  private JsonNode advice;
  private boolean successful;

  public HandshakeResponseMessage (String version, String channel, JsonNode ext, String id, String minimumVersion, String[] supportedConnectionTypes, JsonNode advice, boolean successful) {

    super(version, channel, ext, id, minimumVersion);

    this.supportedConnectionTypes = supportedConnectionTypes;
    this.advice = advice;
    this.successful = successful;
  }

  @XmlElement(name = "advice")
  public JsonNode getAdvice () {

    return advice;
  }

  public void setAdvice (JsonNode advice) {

    this.advice = advice;
  }

  @XmlElement(name = "supportedConnectionTypes")
  public String[] getSupportedConnectionTypes () {

    return supportedConnectionTypes;
  }

  public void setSupportedConnectionTypes (String[] supportedConnectionTypes) {

    this.supportedConnectionTypes = supportedConnectionTypes;
  }

  @XmlElement(name = "successful")
  public boolean isSuccessful () {

    return successful;
  }

  public void setSuccessful (boolean successful) {

    this.successful = successful;
  }
}
