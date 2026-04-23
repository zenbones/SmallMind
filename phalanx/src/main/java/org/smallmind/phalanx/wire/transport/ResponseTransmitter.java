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
package org.smallmind.phalanx.wire.transport;

/**
 * Contract for sending invocation results (or errors) back to the originating caller over
 * the response channel of the wire transport.
 */
public interface ResponseTransmitter {

  /**
   * Encodes and sends a result or error payload to the caller identified by {@code callerId}.
   *
   * @param callerId      identifier of the originating caller, used to address the response
   * @param correlationId correlation id that ties this response to the original request
   * @param error         {@code true} if {@code result} represents a fault rather than a normal return value
   * @param nativeType    JVM type descriptor of the result value
   * @param result        encoded result or error object to transmit
   * @throws Throwable if the underlying send operation fails
   */
  void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable;
}
