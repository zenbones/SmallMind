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
package org.smallmind.phalanx.wire.transport.jms;

/**
 * Configuration for handling JMS reconnection attempts after failures.
 */
public class ReconnectionPolicy {

  private long reconnectionDelayMilliseconds = 500;
  private int reconnectionAttempts = -1;

  /**
   * Returns the maximum number of reconnection attempts (-1 for unlimited).
   *
   * @return attempt count
   */
  public int getReconnectionAttempts () {

    return reconnectionAttempts;
  }

  /**
   * Sets the maximum reconnection attempts (-1 for unlimited).
   *
   * @param reconnectionAttempts attempt count
   */
  public void setReconnectionAttempts (int reconnectionAttempts) {

    this.reconnectionAttempts = reconnectionAttempts;
  }

  /**
   * Returns the delay between reconnection attempts in milliseconds.
   *
   * @return delay in ms
   */
  public long getReconnectionDelayMilliseconds () {

    return reconnectionDelayMilliseconds;
  }

  /**
   * Sets the delay between reconnection attempts in milliseconds.
   *
   * @param reconnectionDelayMilliseconds delay in ms
   */
  public void setReconnectionDelayMilliseconds (long reconnectionDelayMilliseconds) {

    this.reconnectionDelayMilliseconds = reconnectionDelayMilliseconds;
  }
}
