package org.smallmind.scribe.pen;

import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogIF;

public class SyslogAppender extends AbstractAppender {

  private SyslogIF udpSyslog;
  private int pri;
  private int size;

  public SyslogAppender (String host, int port, int pri) {

    this.pri = pri;

    udpSyslog = Syslog.getInstance("udp");
    udpSyslog.getConfig().setHost(host);
    udpSyslog.getConfig().setPort(port);

    size = udpSyslog.getConfig().getMaxMessageLength();
  }

  public void handleOutput (String output)
    throws Exception {

    if (size < output.length()) {
      updateSize(output.length());
    }

    udpSyslog.log(pri, output);
  }

  private synchronized void updateSize (int upperSize) {

    if (size < upperSize) {
      size = upperSize;
    }
  }

  public boolean requiresFormatter () {

    return true;
  }
}
