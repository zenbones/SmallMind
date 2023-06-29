package org.smallmind.cometd.oumuamua.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.databind.JsonNode;

@XmlRootElement(name = "handshake")
public class HandshakeErrorMessage extends HandshakeResponseMessage {

  private String error;

  public HandshakeErrorMessage (String version, String channel, JsonNode ext, String id, String minimumVersion, String[] supportedConnectionTypes, JsonNode advice, boolean successful, String error) {

    super(version, channel, ext, id, minimumVersion, supportedConnectionTypes, advice, successful);
    this.error = error;
  }

  @XmlElement(name = "error")
  public String getError () {

    return error;
  }

  public void setError (String error) {

    this.error = error;
  }
}
