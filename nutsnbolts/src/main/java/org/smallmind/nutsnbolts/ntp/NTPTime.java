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
package org.smallmind.nutsnbolts.ntp;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

public final class NTPTime {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final String[] hostNames;

  public NTPTime (String[] hostNames) {

    if ((hostNames == null) || (hostNames.length == 0)) {
      throw new IllegalArgumentException("No host names provided");
    }

    this.hostNames = hostNames;
  }

  public final long getOffset (int timeoutMillis)
    throws IOException {

    NTPUDPClient client = new NTPUDPClient();
    client.setDefaultTimeout(timeoutMillis);

    client.open();

    try {

      TimeInfo timeInfo;

      timeInfo = client.getTime(InetAddress.getByName(hostNames[RANDOM.nextInt(hostNames.length)]));
      timeInfo.computeDetails();

      return timeInfo.getOffset();
    }
    finally {
      client.close();
    }
  }
}