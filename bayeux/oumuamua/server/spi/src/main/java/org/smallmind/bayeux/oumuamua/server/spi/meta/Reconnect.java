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
package org.smallmind.bayeux.oumuamua.server.spi.meta;

/**
 * Enumerates the valid values for the Bayeux {@code advice.reconnect} field, directing clients
 * on how to recover from a connection event.
 */
public enum Reconnect {

  /**
   * Client should re-establish the connection using a normal connect request.
   */
  RETRY("retry"),

  /**
   * Client must start over with a full handshake before reconnecting.
   */
  HANDSHAKE("handshake"),

  /**
   * Client should not attempt to reconnect; the server has ended the session.
   */
  NONE("none");

  private final String code;

  /**
   * Binds the constant to its Bayeux wire representation.
   *
   * @param code the exact string value placed in the {@code advice.reconnect} field
   */
  Reconnect (String code) {

    this.code = code;
  }

  /**
   * Returns the Bayeux wire string for this reconnect strategy.
   *
   * @return the reconnect code as it appears in the {@code advice} object sent to clients
   */
  public String getCode () {

    return code;
  }
}
