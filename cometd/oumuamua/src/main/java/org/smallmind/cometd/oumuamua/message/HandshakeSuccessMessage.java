package org.smallmind.cometd.oumuamua.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.databind.JsonNode;

@XmlRootElement(name = "handshake")
public class HandshakeSuccessMessage extends HandshakeResponseMessage {

  private String clientId;

  public HandshakeSuccessMessage (String version, String channel, JsonNode ext, String id, String minimumVersion, String[] supportedConnectionTypes, JsonNode advice, boolean successful, String clientId) {

    super(version, channel, ext, id, minimumVersion, supportedConnectionTypes, advice, successful);
    this.clientId = clientId;
  }

  @XmlElement(name = "clientId")
  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }
}
