package org.smallmind.scribe.pen;

import java.util.Date;
import org.smallmind.scribe.pen.Timestamp;

public class NullTimestamp implements Timestamp {

  public String getTimestamp (Date date) {

    return "";
  }
}
