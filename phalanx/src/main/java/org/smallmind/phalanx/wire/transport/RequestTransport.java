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

import java.util.Map;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.WireContext;

/**
 * Contract for the client-side transport that serializes invocation requests and delivers
 * them to a remote service, then returns the result to the caller.
 */
public interface RequestTransport {

  /**
   * Returns the unique identifier for this transport instance, embedded in outbound requests
   * so that the remote side can address its response back to the correct caller.
   *
   * @return caller id string
   */
  String getCallerId ();

  /**
   * Submits an invocation to the remote service described by {@code route} using the routing
   * strategy encoded in {@code voice}, and returns the result for request/reply conversations.
   *
   * @param voice     voice that encodes the conversation style (in-only vs. request/reply) and routing hints
   * @param route     route identifying the target service, version, and function
   * @param arguments named argument map to include in the request payload
   * @param contexts  wire contexts to propagate to the remote side
   * @return the decoded return value for request/reply calls, or {@code null} for in-only calls
   * @throws Throwable if submission fails, the call times out, or the remote side reports an error
   */
  Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable;

  /**
   * Delivers an inbound result signal to the pending callback identified by {@code correlationId}.
   *
   * @param correlationId id that ties the result to the original outbound request
   * @param resultSignal  signal carrying the remote result or error
   */
  void completeCallback (String correlationId, ResultSignal resultSignal);

  /**
   * Shuts down this transport and releases all associated resources.
   *
   * @throws Exception if shutdown fails
   */
  void close ()
    throws Exception;
}
