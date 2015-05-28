package org.smallmind.throng.wire;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.smallmind.nutsnbolts.context.Context;

@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class WireContext implements Context, Serializable {

}
