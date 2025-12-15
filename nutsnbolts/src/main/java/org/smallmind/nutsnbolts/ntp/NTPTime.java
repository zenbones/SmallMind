/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

/**
 * Lightweight NTP client that computes the clock offset against one of a set of configured NTP hosts.
 */
public final class NTPTime {

  private final String[] hostNames;

  /**
   * Creates an NTP time helper using the provided host names. At least one host is required.
   *
   * @param hostNames array of NTP host names to query
   * @throws IllegalArgumentException if no host names are provided
   */
  public NTPTime (String[] hostNames) {

    if ((hostNames == null) || (hostNames.length == 0)) {
      throw new IllegalArgumentException("No host names provided");
    }

    this.hostNames = hostNames;
  }

  /**
   * Queries a random NTP host from the configured list and returns the computed clock offset
   * relative to the local system clock.
   *
   * @param timeoutMillis socket timeout in milliseconds for the NTP request
   * @return the offset in milliseconds; positive indicates server time is ahead of local time
   * @throws IOException if the NTP query fails
   */
  public long getOffset (int timeoutMillis)
    throws IOException {

    try (NTPUDPClient client = new NTPUDPClient()) {

      TimeInfo timeInfo;

      client.setDefaultTimeout(Duration.of(timeoutMillis, ChronoUnit.MILLIS));
      client.open();

      timeInfo = client.getTime(InetAddress.getByName(hostNames[ThreadLocalRandom.current().nextInt(hostNames.length)]));
      timeInfo.computeDetails();

      return timeInfo.getOffset();
    }
  }
}
