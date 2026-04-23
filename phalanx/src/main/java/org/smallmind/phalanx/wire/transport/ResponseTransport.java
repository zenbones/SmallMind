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
 * Contract for the server-side transport that receives inbound invocation requests,
 * dispatches them to registered {@link WiredService} implementations, and returns results
 * to the caller via the response channel.
 */
public interface ResponseTransport {

  /**
   * Returns the unique instance id assigned to this transport, used by callers to address
   * whisper (point-to-point) requests to a specific node.
   *
   * @return unique instance identifier for this transport node
   */
  String getInstanceId ();

  /**
   * Registers a {@link WiredService} implementation to handle inbound requests for the given interface.
   *
   * @param serviceInterface interface that identifies which inbound requests to route here
   * @param targetService    wired service wrapper around the implementation
   * @return a registration token or subscription id returned by the underlying broker
   * @throws Exception if registration with the underlying transport fails
   */
  String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception;

  /**
   * Returns the current lifecycle state of this transport.
   *
   * @return current {@link TransportState}
   */
  TransportState getState ();

  /**
   * Activates request consumption; the transport begins dispatching inbound requests.
   *
   * @throws Exception if activation fails
   */
  void play ()
    throws Exception;

  /**
   * Suspends request consumption without closing the transport; inbound requests are held or discarded
   * depending on the underlying broker.
   *
   * @throws Exception if suspension fails
   */
  void pause ()
    throws Exception;

  /**
   * Shuts down this transport, stops consuming requests, and releases all associated resources.
   *
   * @throws Exception if shutdown fails
   */
  void close ()
    throws Exception;
}
