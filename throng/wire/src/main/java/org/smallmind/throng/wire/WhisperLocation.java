package org.smallmind.throng.wire;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "whisper")
public class WhisperLocation extends TalkLocation {

  private String instanceId;

  public WhisperLocation () {

  }

  public WhisperLocation (String instanceId, int version, String service, Function function) {

    super(version, service, function);

    this.instanceId = instanceId;
  }

  @Override
  @XmlTransient
  public LocationType getType () {

    return LocationType.WHISPER;
  }

  @XmlElement(name = "instanceId", required = true, nillable = false)
  public String getInstanceId () {

    return instanceId;
  }

  public void setInstanceId (String instanceId) {

    this.instanceId = instanceId;
  }
}
