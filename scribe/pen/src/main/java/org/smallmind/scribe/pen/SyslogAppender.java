/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
